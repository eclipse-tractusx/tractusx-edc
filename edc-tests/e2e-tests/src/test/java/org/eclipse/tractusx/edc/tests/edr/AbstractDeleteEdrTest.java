/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.tests.edr;

import jakarta.json.Json;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.tractusx.edc.lifecycle.tx.TxParticipant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntryStates.EXPIRED;
import static org.eclipse.tractusx.edc.helpers.PolicyHelperFunctions.bpnGroupPolicy;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_BPN;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_NAME;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_BPN;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_NAME;
import static org.eclipse.tractusx.edc.tests.TestCommon.ASYNC_TIMEOUT;

public abstract class AbstractDeleteEdrTest {

    protected static final TxParticipant SOKRATES = TxParticipant.Builder.newInstance()
            .name(SOKRATES_NAME)
            .id(SOKRATES_BPN)
            .build();

    protected static final TxParticipant PLATO = TxParticipant.Builder.newInstance()
            .name(PLATO_NAME)
            .id(PLATO_BPN)
            .build();

    @Test
    @DisplayName("Verify that expired EDR are deleted")
    void negotiateEdr_shouldRemoveExpiredEdrs() throws IOException {

        var assetId = UUID.randomUUID().toString();

        var authCodeHeaderName = "test-authkey";
        var authCode = "test-authcode";

        Map<String, Object> dataAddress = Map.of(
                "name", "transfer-test",
                "baseUrl", "http://test:8080",
                "type", "HttpData",
                "contentType", "application/json",
                "authKey", authCodeHeaderName,
                "authCode", authCode
        );
        PLATO.createAsset(assetId, Map.of(), dataAddress);

        PLATO.storeBusinessPartner(SOKRATES.getBpn(), "test-group1", "test-group2");
        var accessPolicy = PLATO.createPolicyDefinition(bpnGroupPolicy(Operator.NEQ, "forbidden-policy"));
        var contractPolicy = PLATO.createPolicyDefinition(bpnGroupPolicy(Operator.EQ, "test-group1", "test-group2"));
        PLATO.createContractDefinition(assetId, "def-1", accessPolicy, contractPolicy);

        var callbacks = Json.createArrayBuilder()
                .build();

        SOKRATES.edrs().negotiateEdr(PLATO, assetId, callbacks);

        var expired = new ArrayList<String>();

        await().atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    var edrCaches = SOKRATES.edrs().getEdrEntriesByAssetId(assetId);
                    var localExpired = edrCaches.stream()
                            .filter(json -> json.asJsonObject().getJsonString("tx:edrState").getString().equals(EXPIRED.name()))
                            .map(json -> json.asJsonObject().getJsonString("transferProcessId").getString())
                            .toList();
                    assertThat(localExpired).hasSizeGreaterThan(0);
                    expired.add(localExpired.get(0));
                });

        await().atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> expired.forEach((id) -> SOKRATES.edrs().getEdrRequest(id).statusCode(404)));

    }

}
