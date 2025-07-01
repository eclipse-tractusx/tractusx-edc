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

import com.fasterxml.jackson.core.JsonProcessingException;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.edc.http.spi.EdcHttpClient;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Optional extension that logs EDC business events by direct HTTP call to the OpenTelemetry Collector.
 * Should only be used when observability is enabled.
 */
public class EventLoggingSubscriber implements EventSubscriber {
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private final TypeManager typeManager;
    private final Monitor monitor;
    private final EdcHttpClient httpClient;
    private static final String OTEL_LOGS_ENDPOINT = System.getProperty("otel.exporter.otlp.endpoint", "http://umbrella-opentelemetry-collector.umbrella:4318") + "/v1/logs";
    private static final String OTEL_SERVICE_NAME = System.getenv("OTEL_SERVICE_NAME") != null ? System.getenv("OTEL_SERVICE_NAME") : "unknown_service";

    public EventLoggingSubscriber(TypeManager typeManager, Monitor monitor, EdcHttpClient httpClient) {
        this.typeManager = typeManager;
        this.monitor = monitor;
        this.httpClient = httpClient;
    }


    @Override
    public <E extends Event> void on(EventEnvelope<E> event) {
        var eventPayload = typeManager.writeValueAsString(event.getPayload());
        var logMessage = String.format("Event happened with ID %s and Type %s and data %s", event.getId(), event.getPayload().getClass().getName(), eventPayload);
        var resourceLog = createResourceLog(logMessage, event.getPayload().getClass().getName());
        try (var response = httpClient.execute(createRequest(new OtelRequestWrapper(List.of(resourceLog))))) {
            if (!response.isSuccessful()) {
                monitor.warning(String.format("HTTP call to otel collector has failed with status: %d", response.code()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private Request createRequest(OtelRequestWrapper messageWrapper) {
        var requestBuilder = new Request.Builder();
        var jsonWritter = typeManager.getMapper().writerWithDefaultPrettyPrinter();
        try {
            return requestBuilder.post(RequestBody.create(jsonWritter.writeValueAsString(messageWrapper), JSON_MEDIA_TYPE))
                    .url(OTEL_LOGS_ENDPOINT)
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private ResourceLog createResourceLog(String message, String eventName) {
        return new ResourceLog(
                new Resource(List.of(new Attribute("service.name", new StringValue(OTEL_SERVICE_NAME)))),
                List.of(new ScopeLog(new Scope("default scope", "1.0.0", new ArrayList<>()), List.of(new SubscribedEventLogRecord(new StringValue(message), eventName))))
            );
    }
}
