/*
 * Copyright (c) 2025 Schaeffler AG
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.edc.eventsubscriber;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.tractusx.edc.agreements.retirement.spi.event.ContractAgreementReactivated;
import org.eclipse.tractusx.edc.eventsubscriber.otelutil.Attribute;
import org.eclipse.tractusx.edc.eventsubscriber.otelutil.OtelRequestWrapper;
import org.eclipse.tractusx.edc.eventsubscriber.otelutil.Resource;
import org.eclipse.tractusx.edc.eventsubscriber.otelutil.ResourceLog;
import org.eclipse.tractusx.edc.eventsubscriber.otelutil.ScopeLog;
import org.eclipse.tractusx.edc.eventsubscriber.otelutil.StringValue;
import org.eclipse.tractusx.edc.eventsubscriber.otelutil.SubscribedEventLogRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventLoggingSubscriberTest {
    private final EdcHttpClient mockedHttpClient = mock();
    private final TypeManager typeManager = new JacksonTypeManager();
    private final Monitor mockedMonitor = mock();
    private EventLoggingSubscriber eventLoggingSubscriber;

    @BeforeEach
    void setup() {
        eventLoggingSubscriber = new EventLoggingSubscriber(typeManager, mockedMonitor, mockedHttpClient, "http://uri.com", "unknown_service");
    }


    @Test
    void on_unsuccessfulHttpCall_logsWarningMessage() throws IOException {
        var req = new Request(HttpUrl.parse("http://uri.com"), "POST", Headers.of(), RequestBody.create(new byte[] {}), Map.of());
        var body = ResponseBody.create("{}", MediaType.get("application/json"));

        when(mockedHttpClient.execute(any())).thenReturn(new Response.Builder().body(body).request(req).protocol(Protocol.HTTP_2).message("message").code(500).build());
        var event = ContractAgreementReactivated.Builder.newInstance().contractAgreementId(UUID.randomUUID().toString()).build();

        var envelope = EventEnvelope.Builder
                .newInstance()
                .id(UUID.randomUUID().toString())
                .at(System.currentTimeMillis())
                .payload(event)
                .build();
        eventLoggingSubscriber.on(envelope);

        verify(mockedMonitor, times(1)).warning(anyString());
    }

    @Test
    void on_successfulHttpCall_responseIsProperlyClosedAndNoSingleWarningIsLogged() throws IOException {
        Response mockedResponse = mock();

        when(mockedHttpClient.execute(any())).thenReturn(mockedResponse);
        when(mockedResponse.isSuccessful()).thenReturn(true);


        var event = ContractAgreementReactivated.Builder.newInstance().contractAgreementId(UUID.randomUUID().toString()).build();
        var envelope = EventEnvelope.Builder
                .newInstance()
                .id(UUID.randomUUID().toString())
                .at(System.currentTimeMillis())
                .payload(event)
                .build();

        eventLoggingSubscriber.on(envelope);
        verify(mockedMonitor, times(0)).warning(anyString());
        verify(mockedResponse, times(1)).close();
    }


    @Test
    void on_httpCall_BodyIsProperlyMappedToOtelLogsJsonTemplate() throws IOException {
        var captor = ArgumentCaptor.forClass(Request.class);
        var mockedResponse = mock(Response.class);
        var objectMapper = new ObjectMapper();
        var buffer = new Buffer();
        when(mockedHttpClient.execute(any())).thenReturn(mockedResponse);


        var event = ContractAgreementReactivated.Builder.newInstance().contractAgreementId(UUID.randomUUID().toString()).build();
        var envelope = EventEnvelope.Builder
                .newInstance()
                .id(UUID.randomUUID().toString())
                .at(System.currentTimeMillis())
                .payload(event)
                .build();

        eventLoggingSubscriber.on(envelope);
        verify(mockedHttpClient).execute(captor.capture());
        var request = captor.getValue();
        request.body().writeTo(buffer);
        var bodyString = buffer.readUtf8();
        var mappedRequestBody = objectMapper.readValue(bodyString, OtelRequestWrapper.class);
        var payload = objectMapper.writeValueAsString(envelope.getPayload());


        Assertions.assertNotNull(mappedRequestBody);
        assertThat(mappedRequestBody.resourceLogs())
                .isNotEmpty()
            .extracting(ResourceLog::resource)
                .isNotEmpty()
            .extracting(Resource::attributes)
                .isNotEmpty()
                .contains(List.of(new Attribute("service.name", new StringValue("unknown_service"))));

        var expectedMesage = String.format("Event happened with ID %s and Type %s and data %s", envelope.getId(), envelope.getPayload().getClass().getName(), payload);

        assertThat(mappedRequestBody.resourceLogs())
                .flatExtracting(ResourceLog::scopeLogs)
                    .isNotEmpty()
                .flatExtracting(ScopeLog::logRecords)
                    .isNotEmpty()
                .flatExtracting(SubscribedEventLogRecord::eventName, SubscribedEventLogRecord::body)
                .contains(
                        event.getClass().getName(),
                        new StringValue(expectedMesage)
                );
    }
}
