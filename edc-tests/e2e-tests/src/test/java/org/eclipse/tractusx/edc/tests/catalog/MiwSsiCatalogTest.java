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

package org.eclipse.tractusx.edc.tests.catalog;

import org.eclipse.tractusx.edc.lifecycle.MiwParticipant;
import org.eclipse.tractusx.edc.lifecycle.ParticipantRuntime;
import org.eclipse.tractusx.edc.tag.MiwIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.test.system.utils.PolicyFixtures.noConstraintPolicy;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_NAMESPACE;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.SOKRATES_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.SOKRATES_NAME;
import static org.eclipse.tractusx.edc.tests.helpers.CatalogHelperFunctions.getDatasetAssetId;
import static org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions.frameworkPolicy;

@MiwIntegrationTest
public class MiwSsiCatalogTest {

    protected static final String MIW_SOKRATES_URL = "http://localhost:8000";
    protected static final String OAUTH_TOKEN_URL = "http://localhost:8080/realms/miw_test/protocol/openid-connect/token";
    protected static final MiwParticipant SOKRATES = MiwParticipant.Builder.newInstance()
            .name(SOKRATES_NAME)
            .id(SOKRATES_BPN)
            .miwUri(MIW_SOKRATES_URL)
            .oauth2Uri(OAUTH_TOKEN_URL)
            .build();


    @RegisterExtension
    protected static final ParticipantRuntime SOKRATES_RUNTIME = new ParticipantRuntime(
            ":edc-tests:runtime:runtime-memory-ssi",
            SOKRATES.getName(),
            SOKRATES.getConfiguration()
    );

    @Test
    @DisplayName("Verify that Sokrates receives only the offers he is permitted to")
    void requestCatalog_fulfillsPolicy_shouldReturnOffer() {
        // arrange
        SOKRATES.createAsset("test-asset");
        SOKRATES.createAsset("test-asset-1");

        var bpnAccessPolicy = frameworkPolicy(Map.of(TX_NAMESPACE + "BPN", "active"));
        var dismantlerAccessPolicy = frameworkPolicy(Map.of(TX_NAMESPACE + "Dismantler", "active"));

        var bpnAccessId = SOKRATES.createPolicyDefinition(bpnAccessPolicy);
        var contractPolicyId = SOKRATES.createPolicyDefinition(noConstraintPolicy());
        var dismantlerAccessPolicyId = SOKRATES.createPolicyDefinition(dismantlerAccessPolicy);

        SOKRATES.createContractDefinition("test-asset", "test-def", bpnAccessId, contractPolicyId);
        SOKRATES.createContractDefinition("test-asset-1", "test-def-2", dismantlerAccessPolicyId, contractPolicyId);

        // act
        var catalog = SOKRATES.getCatalogDatasets(SOKRATES);

        // assert
        assertThat(catalog).isNotEmpty()
                .hasSize(1)
                .allSatisfy(co -> {
                    assertThat(getDatasetAssetId(co)).isEqualTo("test-asset");
                });

    }

}
