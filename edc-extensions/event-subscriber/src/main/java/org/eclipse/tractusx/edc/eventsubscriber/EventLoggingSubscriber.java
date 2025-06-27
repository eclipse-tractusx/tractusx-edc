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
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.tractusx.edc.eventsubscriber.otelutil.Attribute;
import org.eclipse.tractusx.edc.eventsubscriber.otelutil.OtelRequestWrapper;
import org.eclipse.tractusx.edc.eventsubscriber.otelutil.Resource;
import org.eclipse.tractusx.edc.eventsubscriber.otelutil.ResourceLog;
import org.eclipse.tractusx.edc.eventsubscriber.otelutil.Scope;
import org.eclipse.tractusx.edc.eventsubscriber.otelutil.ScopeLog;
import org.eclipse.tractusx.edc.eventsubscriber.otelutil.StringValue;
import org.eclipse.tractusx.edc.eventsubscriber.otelutil.SubscribedEventLogRecord;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EventLoggingSubscriber implements EventSubscriber {
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final ObjectWriter JSON_WRITTER = new ObjectMapper().writer().withDefaultPrettyPrinter();
    private final TypeManager typeManager;
    private final Monitor monitor;
    private OkHttpClient client = new OkHttpClient();
    private static final Logger LOGGER = LoggerFactory.getLogger(EventLoggingSubscriber.class);
    private static final boolean IS_OTEL_ENABLED = Boolean.parseBoolean(System.getProperty("otel.javaagent.enabled", "true"));
    private static final String OTEL_LOGS_ENDPOINT = System.getProperty("otel.exporter.otlp.endpoint", "http://umbrella-opentelemetry-collector.umbrella:4318") + "/v1/logs";
    private static final String OTEL_SERVICE_NAME = System.getenv("OTEL_SERVICE_NAME") != null ? System.getenv("OTEL_SERVICE_NAME") : "unknown_service";

    EventLoggingSubscriber(TypeManager typeManager, Monitor monitor) {
        this.typeManager = typeManager;
        this.monitor = monitor;
    }


    @Override
    public <E extends Event> void on(EventEnvelope<E> event) {
        if (IS_OTEL_ENABLED) {
            var eventPayload = typeManager.writeValueAsString(event.getPayload());
            var logMessage = String.format("Event happened with ID %s and Type %s and data %s", event.getId(), event.getPayload().getClass().getName(), eventPayload);
            var logEvent = createLogEvent(logMessage, event.getPayload().getClass().getName());
            try (var response = client.newCall(createRequest(new OtelRequestWrapper(List.of(logEvent)))).execute()) {
                if (!response.isSuccessful()) {
                    response.peekBody(response.body().contentLength());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @NotNull
    private Request createRequest(OtelRequestWrapper messageWrapper) {
        var requestBuilder = new Request.Builder();
        try {
            LOGGER.error(String.format("ENDPOINT: %s\nSERVICE_NAME: %s\nOTEL_ENABLED: %b", OTEL_LOGS_ENDPOINT, OTEL_SERVICE_NAME, IS_OTEL_ENABLED));
            return requestBuilder.post(RequestBody.create(JSON_WRITTER.writeValueAsString(messageWrapper), JSON_MEDIA_TYPE))
                    .url(OTEL_LOGS_ENDPOINT)
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private ResourceLog createLogEvent(String message, String eventName) {
        return new ResourceLog(
                new Resource(List.of(new Attribute("service.name", new StringValue(OTEL_SERVICE_NAME)))),
                List.of(new ScopeLog(new Scope("default scope", "1.0.0", new ArrayList<>()), List.of(new SubscribedEventLogRecord(new StringValue(message), eventName))))
            );
    }
}
