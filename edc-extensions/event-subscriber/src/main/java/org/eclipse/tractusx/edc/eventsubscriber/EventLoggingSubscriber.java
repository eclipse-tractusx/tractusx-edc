/*
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.tractusx.edc.eventsubscriber.otelutil.Attribute;
import org.eclipse.tractusx.edc.eventsubscriber.otelutil.Resource;
import org.eclipse.tractusx.edc.eventsubscriber.otelutil.ResourceLog;
import org.eclipse.tractusx.edc.eventsubscriber.otelutil.Scope;
import org.eclipse.tractusx.edc.eventsubscriber.otelutil.ScopeLog;
import org.eclipse.tractusx.edc.eventsubscriber.otelutil.StringValue;
import org.eclipse.tractusx.edc.eventsubscriber.otelutil.SubscribedEventLogRecord;
import org.eclipse.tractusx.edc.eventsubscriber.otelutil.Wrapper;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EventLoggingSubscriber implements EventSubscriber {

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final ObjectWriter MAPPER = new ObjectMapper().writer().withDefaultPrettyPrinter();
    private static Logger logger = LoggerFactory.getLogger(EventSubscriber.class);
    private final TypeManager typeManager;
    private final Monitor monitor;
    private OkHttpClient client = new OkHttpClient();

    EventLoggingSubscriber(TypeManager typeManager, Monitor monitor) {
        this.typeManager = typeManager;
        this.monitor = monitor;
    }


    @Override
    public <E extends Event> void on(EventEnvelope<E> event) {
        var json = typeManager.writeValueAsString(event.getPayload());
        var log = String.format("Event happened with ID %s and Type %s and data %s", event.getId(), event.getPayload().getClass().getName(), json);
        var req = createLogEvent(log, event.getPayload().getClass().getName());
        client.newCall(createRequest(new Wrapper(List.of(req)))).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                logger.error("failure");
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                logger.info("SUCCESS");
            }
        });
    }

    @NotNull
    private Request createRequest(Wrapper messageWrapper) {
        var requestBuilder = new Request.Builder();
        Request request = null;
        try {
            request = requestBuilder.post(RequestBody.create(MAPPER.writeValueAsString(messageWrapper), JSON))
                    .url("http://umbrella-opentelemetry-collector.umbrella:4318/v1/logs")
                    .build();
            logger.error(MAPPER.writeValueAsString(messageWrapper));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return request;
    }

    private ResourceLog createLogEvent(String message, String eventName) {
        return new ResourceLog(
                new Resource(List.of(new Attribute("service.name", new StringValue("unknown_service")))),
                List.of(new ScopeLog(new Scope("name", "1.0.0", new ArrayList<>()), List.of(new SubscribedEventLogRecord(new StringValue(message), eventName))))
            );
    }
}
