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

package org.eclipse.tractusx.edc.tests.edr;

import jakarta.json.Json;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.assertj.core.api.Condition;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessStarted;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.tractusx.edc.lifecycle.Participant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.anyOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntryStates.EXPIRED;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntryStates.NEGOTIATED;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntryStates.REFRESHING;
import static org.eclipse.tractusx.edc.helpers.EdrNegotiationHelperFunctions.createCallback;
import static org.eclipse.tractusx.edc.helpers.EdrNegotiationHelperFunctions.createEvent;
import static org.eclipse.tractusx.edc.helpers.PolicyHelperFunctions.businessPartnerGroupPolicy;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_BPN;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_NAME;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_BPN;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_NAME;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.platoConfiguration;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.sokratesConfiguration;
import static org.eclipse.tractusx.edc.tests.TestCommon.ASYNC_POLL_INTERVAL;
import static org.eclipse.tractusx.edc.tests.TestCommon.ASYNC_TIMEOUT;
import static org.eclipse.tractusx.edc.tests.edr.TestFunctions.waitForEvent;

public abstract class AbstractRenewalEdrTest {

    protected static final Participant SOKRATES = new Participant(SOKRATES_NAME, SOKRATES_BPN, sokratesConfiguration());
    protected static final Participant PLATO = new Participant(PLATO_NAME, PLATO_BPN, platoConfiguration());

    MockWebServer server;

    @BeforeEach
    void setup() {
        server = new MockWebServer();
    }

    @Test
    @DisplayName("Verify that the EDR is renewed")
    void negotiateEdr_shouldRenewTheEdr() throws IOException {

        var expectedEvents = List.of(
                createEvent(TransferProcessStarted.class),
                createEvent(TransferProcessStarted.class));

        var assetId = UUID.randomUUID().toString();
        var url = server.url("/mock/api");
        server.start();

        var authCodeHeaderName = "test-authkey";
        var authCode = "test-authcode";
        PLATO.createAsset(assetId, Json.createObjectBuilder().build(), Json.createObjectBuilder()
                .add(EDC_NAMESPACE + "type", "HttpData")
                .add(EDC_NAMESPACE + "contentType", "application/json")
                .add(EDC_NAMESPACE + "baseUrl", url.toString())
                .add(EDC_NAMESPACE + "authKey", authCodeHeaderName)
                .add(EDC_NAMESPACE + "authCode", authCode)
                .build());

        PLATO.storeBusinessPartner(SOKRATES.getBpn(), "test-group1", "test-group2");
        PLATO.createPolicy(businessPartnerGroupPolicy("policy-1", Operator.IS_NONE_OF, "forbidden-policy"));
        PLATO.createPolicy(businessPartnerGroupPolicy("policy-2", Operator.IS_ANY_OF, "test-group1", "test-group2"));
        PLATO.createContractDefinition(assetId, "def-1", "policy-1", "policy-2");

        var callbacks = Json.createArrayBuilder()
                .add(createCallback(url.toString(), true, Set.of("transfer.process.started")))
                .build();

        expectedEvents.forEach(event -> server.enqueue(new MockResponse()));

        SOKRATES.negotiateEdr(PLATO, assetId, callbacks);

        var events = expectedEvents.stream()
                .map(receivedEvent -> waitForEvent(server, receivedEvent))
                .collect(Collectors.toList());

        assertThat(expectedEvents).usingRecursiveFieldByFieldElementComparator().containsAll(events);

        var edrCachesBuilder = Json.createArrayBuilder();

        await().atMost(ASYNC_TIMEOUT)
                .pollInterval(ASYNC_POLL_INTERVAL)
                .untilAsserted(() -> {
                    var localEdrCaches = SOKRATES.getEdrEntriesByAssetId(assetId);
                    assertThat(localEdrCaches).hasSizeGreaterThan(1);
                    localEdrCaches.forEach(edrCachesBuilder::add);
                });

        var edrCaches = edrCachesBuilder.build();

        assertThat(edrCaches)
                .extracting(json -> json.asJsonObject().getJsonString("tx:edrState").getString())
                .areAtMost(1, anyOf(stateCondition(NEGOTIATED.name(), "Negotiated"), stateCondition(REFRESHING.name(), "Refreshing")))
                .areAtLeast(1, stateCondition(EXPIRED.name(), "Expired"));

        var transferProcessId = edrCaches.stream()
                .filter(json -> json.asJsonObject().getJsonString("tx:edrState").getString().equals(EXPIRED.name()))
                .map(json -> json.asJsonObject().getJsonString("transferProcessId").getString())
                .findFirst()
                .orElseThrow();

        await().pollInterval(fibonacci())
                .atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    var tpState = SOKRATES.getTransferProcessState(transferProcessId);
                    assertThat(tpState).isNotNull().isEqualTo(TransferProcessStates.COMPLETED.toString());
                });
    }


    @AfterEach
    void teardown() throws IOException {
        server.shutdown();
    }


    private Condition<String> stateCondition(String value, String description) {
        return new Condition<>(m -> m.equals(value), description);
    }

}
