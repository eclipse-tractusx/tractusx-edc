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
import okhttp3.mockwebserver.MockWebServer;
import org.assertj.core.api.Condition;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.tractusx.edc.lifecycle.Participant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.UUID;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntryStates.EXPIRED;
import static org.eclipse.tractusx.edc.helpers.PolicyHelperFunctions.businessPartnerGroupPolicy;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_BPN;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_NAME;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_BPN;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_NAME;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.platoConfiguration;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.sokratesConfiguration;

public abstract class AbstractDeleteEdrTest {

    protected static final Participant SOKRATES = new Participant(SOKRATES_NAME, SOKRATES_BPN, sokratesConfiguration());
    protected static final Participant PLATO = new Participant(PLATO_NAME, PLATO_BPN, platoConfiguration());
    private static final Duration ASYNC_TIMEOUT = ofSeconds(45);
    MockWebServer server;

    @BeforeEach
    void setup() {
        server = new MockWebServer();
    }

    @Test
    @DisplayName("Verify that expired EDR are deleted")
    void negotiateEdr_shouldRemoveExpiredEdrs() throws IOException {

        var assetId = UUID.randomUUID().toString();

        var authCodeHeaderName = "test-authkey";
        var authCode = "test-authcode";
        PLATO.createAsset(assetId, Json.createObjectBuilder().build(), Json.createObjectBuilder()
                .add(EDC_NAMESPACE + "type", "HttpData")
                .add(EDC_NAMESPACE + "contentType", "application/json")
                .add(EDC_NAMESPACE + "baseUrl", "http://test:8080")
                .add(EDC_NAMESPACE + "authKey", authCodeHeaderName)
                .add(EDC_NAMESPACE + "authCode", authCode)
                .build());

        PLATO.storeBusinessPartner(SOKRATES.getBpn(), "test-group1", "test-group2");
        PLATO.createPolicy(businessPartnerGroupPolicy("policy-1", Operator.NEQ, "forbidden-policy"));
        PLATO.createPolicy(businessPartnerGroupPolicy("policy-2", Operator.EQ, "test-group1", "test-group2"));
        PLATO.createContractDefinition(assetId, "def-1", "policy-1", "policy-2");

        var callbacks = Json.createArrayBuilder()
                .build();

        SOKRATES.negotiateEdr(PLATO, assetId, callbacks);

        var expired = new ArrayList<String>();

        await().atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    var edrCaches = SOKRATES.getEdrEntriesByAssetId(assetId);
                    var localExpired = edrCaches.stream()
                            .filter(json -> json.asJsonObject().getJsonString("tx:edrState").getString().equals(EXPIRED.name()))
                            .map(json -> json.asJsonObject().getJsonString("transferProcessId").getString())
                            .toList();
                    assertThat(localExpired).hasSizeGreaterThan(0);
                    expired.add(localExpired.get(0));
                });

        await().atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> expired.forEach((id) -> SOKRATES.getEdrRequest(id).statusCode(404)));

    }

    @AfterEach
    void teardown() throws IOException {
        server.shutdown();
    }


    private Condition<String> stateCondition(String value, String description) {
        return new Condition<>(m -> m.equals(value), description);
    }

}
