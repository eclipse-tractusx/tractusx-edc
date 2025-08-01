/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
 ********************************************************************************/

package org.eclipse.tractusx.edc.tests.participant;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.test.system.utils.LazySupplier;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.util.io.Ports;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.mockserver.model.HttpRequest.request;

/**
 * Extension of {@link TractusxParticipantBase} with Transfer specific configuration
 */
public class TransferParticipant extends TractusxParticipantBase {

    private EventSubscription eventSubscription;

    public JsonObject waitForEvent(String eventType) {
        if (eventSubscription == null) {
            throw new IllegalStateException("Event subscription not enabled on this participant");
        }
        return eventSubscription.waitForEvent(eventType);
    }

    @Override
    public Config getConfig() {
        var config = super.getConfig();
        if (eventSubscription == null) {
            return config;
        }

        return config.merge(ConfigFactory.fromMap(Map.of(
                "edc.callback.default.events", "contract, bpn",
                "edc.callback.default.uri", "http://localhost:" + eventSubscription.getPort(),
                "edc.callback.default.transactional", "true"
        )));
    }

    public static class Builder extends TractusxParticipantBase.Builder<TransferParticipant, Builder> {

        protected Builder() {
            super(new TransferParticipant());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        @Override
        public TransferParticipant build() {
            super.build();
            return participant;
        }

        public Builder enableEventSubscription() {
            participant.eventSubscription = new EventSubscription(participant.timeout);
            return this;
        }
    }

    private static class EventSubscription {
        private final LazySupplier<Integer> eventReceiverPort = new LazySupplier<>(Ports::getFreePort);
        private final ClientAndServer server = ClientAndServer.startClientAndServer(eventReceiverPort.get());
        private final BlockingQueue<JsonObject> events = new LinkedBlockingQueue<>();
        private final Duration timeout;

        EventSubscription(Duration timeout) {
            this.timeout = timeout;
            server.when(request()).respond(httpRequest -> {
                var bodyAsRawBytes = httpRequest.getBodyAsRawBytes();
                var event = Json.createReader(new ByteArrayInputStream(bodyAsRawBytes)).readObject();
                events.add(event);
                return HttpResponse.response();
            });
        }

        public JsonObject waitForEvent(String eventType) {
            try {
                do {
                    var event = events.poll(timeout.getSeconds(), TimeUnit.SECONDS);
                    if (event == null) {
                        throw new TimeoutException("No event of type " + eventType + " received");
                    }
                    if (Objects.equals(event.getString("type"), eventType)) {
                        return event;
                    }
                } while (true);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public int getPort() {
            return eventReceiverPort.get();
        }
    }
}
