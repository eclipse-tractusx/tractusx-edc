/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.tests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.eclipse.edc.connector.contract.spi.event.contractnegotiation.ContractNegotiationAgreed;
import org.eclipse.edc.connector.contract.spi.event.contractnegotiation.ContractNegotiationFinalized;
import org.eclipse.edc.connector.contract.spi.event.contractnegotiation.ContractNegotiationInitiated;
import org.eclipse.edc.connector.contract.spi.event.contractnegotiation.ContractNegotiationRequested;
import org.eclipse.edc.connector.contract.spi.event.contractnegotiation.ContractNegotiationVerified;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessCompleted;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessInitiated;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessProvisioned;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessRequested;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessStarted;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.tractusx.edc.lifecycle.MultiRuntimeTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.tractusx.edc.helpers.EdrNegotiationHelperFunctions.createCallback;
import static org.eclipse.tractusx.edc.helpers.PolicyHelperFunctions.businessPartnerNumberPolicy;

@EndToEndTest
public class NegotiateEdrTest extends MultiRuntimeTest {

    MockWebServer server = new MockWebServer();

    ObjectMapper mapper = new ObjectMapper();


    @Test
    @DisplayName("Verify that the callbacks are invoked when negotiating an EDR")
    void negotiateEdr_shouldInvokeCallbacks() throws IOException {

        var expectedEvents = List.of(
                createEvent(ContractNegotiationInitiated.class),
                createEvent(ContractNegotiationRequested.class),
                createEvent(ContractNegotiationAgreed.class),
                createEvent(ContractNegotiationFinalized.class),
                createEvent(ContractNegotiationVerified.class),
                createEvent(TransferProcessInitiated.class),
                createEvent(TransferProcessProvisioned.class),
                createEvent(TransferProcessRequested.class),
                createEvent(TransferProcessStarted.class),
                createEvent(TransferProcessCompleted.class));

        var assetId = "api-asset-1";
        var url = server.url("/mock/api");
        server.start();

        var authCodeHeaderName = "test-authkey";
        var authCode = "test-authcode";
        plato.createAsset(assetId, Json.createObjectBuilder().build(), Json.createObjectBuilder()
                .add(EDC_NAMESPACE + "type", "HttpData")
                .add(EDC_NAMESPACE + "contentType", "application/json")
                .add(EDC_NAMESPACE + "baseUrl", url.toString())
                .add(EDC_NAMESPACE + "authKey", authCodeHeaderName)
                .add(EDC_NAMESPACE + "authCode", authCode)
                .build());

        plato.createPolicy(businessPartnerNumberPolicy("policy-1", sokrates.getBpn()));
        plato.createPolicy(businessPartnerNumberPolicy("policy-2", sokrates.getBpn()));
        plato.createContractDefinition(assetId, "def-1", "policy-1", "policy-2");


        expectedEvents.forEach(_event -> {
            server.enqueue(new MockResponse());
        });

        var callbacks = Json.createArrayBuilder()
                .add(createCallback(url.toString(), true, Set.of("contract.negotiation", "transfer.process")))
                .build();

        sokrates.negotiateEdr(plato, assetId, callbacks);

        var events = expectedEvents.stream()
                .map(this::waitForEvent)
                .collect(Collectors.toList());

        assertThat(expectedEvents).usingRecursiveFieldByFieldElementComparator().containsAll(events);
    }

    <E extends Event> ReceivedEvent createEvent(Class<E> klass) {
        return ReceivedEvent.Builder.newInstance().type(klass.getSimpleName()).build();
    }

    ReceivedEvent waitForEvent(ReceivedEvent event) {
        try {
            var request = server.takeRequest(20, TimeUnit.SECONDS);
            if (request != null) {
                return mapper.readValue(request.getBody().inputStream(), ReceivedEvent.class);
            } else {
                throw new RuntimeException("Timeout exceeded waiting for events");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ReceivedEvent {
        private String type;

        public String getType() {
            return type;
        }

        @Override
        public String toString() {
            return "ReceivedEvent{" +
                    "type='" + type + '\'' +
                    '}';
        }

        public static class Builder {
            private final ReceivedEvent event;

            private Builder(ReceivedEvent event) {
                this.event = event;
            }

            public static Builder newInstance() {
                return new Builder(new ReceivedEvent());
            }

            public Builder type(String type) {
                this.event.type = type;
                return this;
            }

            public ReceivedEvent build() {
                return event;
            }
        }


    }
}
