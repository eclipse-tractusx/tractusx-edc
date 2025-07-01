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

import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.tractusx.edc.agreements.retirement.spi.event.ContractAgreementReactivated;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

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
        eventLoggingSubscriber = new EventLoggingSubscriber(typeManager, mockedMonitor, mockedHttpClient);
    }


    @Test
    void on_unsuccessfulHttpCall_logsWarningMesssage() throws IOException {
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
}
