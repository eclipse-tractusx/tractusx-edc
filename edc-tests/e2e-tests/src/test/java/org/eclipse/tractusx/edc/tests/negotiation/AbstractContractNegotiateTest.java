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

package org.eclipse.tractusx.edc.tests.negotiation;

import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.tractusx.edc.lifecycle.tx.TxParticipant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.tractusx.edc.helpers.PolicyHelperFunctions.TX_NAMESPACE;
import static org.eclipse.tractusx.edc.helpers.PolicyHelperFunctions.bpnGroupPolicy;
import static org.eclipse.tractusx.edc.helpers.PolicyHelperFunctions.frameworkPolicy;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_BPN;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_NAME;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_BPN;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_NAME;
import static org.eclipse.tractusx.edc.tests.TestCommon.ASYNC_POLL_INTERVAL;
import static org.eclipse.tractusx.edc.tests.TestCommon.ASYNC_TIMEOUT;

public abstract class AbstractContractNegotiateTest {

    protected static final TxParticipant SOKRATES = TxParticipant.Builder.newInstance()
            .name(SOKRATES_NAME)
            .id(SOKRATES_BPN)
            .build();

    protected static final TxParticipant PLATO = TxParticipant.Builder.newInstance()
            .name(PLATO_NAME)
            .id(PLATO_BPN)
            .build();

    @Test
    @DisplayName("Verify contract negotiation fails with wrong policy")
    void contractNegotiation_shouldFail_whenPolicyEvaluationFails() {
        var assetId = "api-asset-1";
        var authCodeHeaderName = "test-authkey";
        var authCode = "test-authcode";

        Map<String, Object> dataAddress = Map.of(
                "name", "transfer-test",
                "baseUrl", "http://testurl",
                "type", "HttpData",
                "contentType", "application/json",
                "authKey", authCodeHeaderName,
                "authCode", authCode
        );

        PLATO.createAsset(assetId, Map.of(), dataAddress);

        PLATO.storeBusinessPartner(SOKRATES.getBpn(), "allowed-group");
        var accessPolicyId = PLATO.createPolicyDefinition(bpnGroupPolicy(Operator.NEQ, "forbidden-group"));
        var contractPolicyId = PLATO.createPolicyDefinition(frameworkPolicy(Map.of(TX_NAMESPACE + "Dismantler", "active")));
        PLATO.createContractDefinition(assetId, "def-1", accessPolicyId, contractPolicyId);

        var negotiationId = SOKRATES.initContractNegotiation(PLATO, assetId);

        // wait for the failed contract negotiation
        await().pollInterval(ASYNC_POLL_INTERVAL)
                .atMost(ASYNC_TIMEOUT)
                .untilAsserted(() -> {
                    var negotiationState = SOKRATES.getContractNegotiationState(negotiationId);
                    assertThat(negotiationState).isEqualTo(ContractNegotiationStates.TERMINATED.toString());
                    var error = SOKRATES.getContractNegotiationError(negotiationId);

                    assertThat(error).isNotNull();
                    assertThat(error).contains("Contract offer is not valid: Policy in scope contract.negotiation not fulfilled");
                });
    }


}
