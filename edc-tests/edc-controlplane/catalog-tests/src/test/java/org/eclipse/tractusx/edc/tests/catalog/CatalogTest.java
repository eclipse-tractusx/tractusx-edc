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

import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.tractusx.edc.tests.participant.TransferParticipant;
import org.eclipse.tractusx.edc.tests.runtimes.ParticipantRuntime;
import org.eclipse.tractusx.edc.tests.runtimes.PgParticipantRuntime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;

import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.noConstraintPolicy;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PLATO_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PLATO_NAME;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.SOKRATES_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.SOKRATES_NAME;
import static org.eclipse.tractusx.edc.tests.helpers.CatalogHelperFunctions.getDatasetAssetId;
import static org.eclipse.tractusx.edc.tests.helpers.CatalogHelperFunctions.getDatasetPolicies;
import static org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions.BUSINESS_PARTNER_LEGACY_EVALUATION_KEY;
import static org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions.bnpPolicy;
import static org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions.bpnGroupPolicy;
import static org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions.frameworkPolicy;
import static org.eclipse.tractusx.edc.tests.helpers.QueryHelperFunctions.createQuery;
import static org.eclipse.tractusx.edc.tests.runtimes.Runtimes.memoryRuntime;
import static org.eclipse.tractusx.edc.tests.runtimes.Runtimes.pgRuntime;

public class CatalogTest {

    protected static final TransferParticipant SOKRATES = TransferParticipant.Builder.newInstance()
            .name(SOKRATES_NAME)
            .id(SOKRATES_BPN)
            .build();


    protected static final TransferParticipant PLATO = TransferParticipant.Builder.newInstance()
            .name(PLATO_NAME)
            .id(PLATO_BPN)
            .build();


    abstract static class Tests {
        @Test
        @DisplayName("Plato gets catalog from Sokrates. No constraints.")
        void requestCatalog_fulfillsPolicy_shouldReturnOffer() {
            // arrange
            SOKRATES.createAsset("test-asset");
            var ap = SOKRATES.createPolicyDefinition(noConstraintPolicy());
            var cp = SOKRATES.createPolicyDefinition(noConstraintPolicy());
            SOKRATES.createContractDefinition("test-asset", "test-def", ap, cp);

            // act
            var catalog = PLATO.getCatalogDatasets(SOKRATES);

            // assert
            assertThat(catalog).isNotEmpty()
                    .hasSize(1)
                    .allSatisfy(co -> {
                        assertThat(getDatasetAssetId(co.asJsonObject())).isEqualTo("test-asset");
                    });

        }

        @Test
        @DisplayName("Verify that Plato receives only the offers he is permitted to (using the legacy BPN validation)")
        void requestCatalog_filteredByBpnLegacy_shouldReject() {
            var onlyPlatoPolicy = bnpPolicy("BPN1", "BPN2", PLATO.getBpn());
            var onlyDiogenesPolicy = bnpPolicy("ARISTOTELES-BPN");

            var onlyPlatoId = SOKRATES.createPolicyDefinition(onlyPlatoPolicy);
            var onlyDiogenesId = SOKRATES.createPolicyDefinition(onlyDiogenesPolicy);
            var noConstraintPolicyId = SOKRATES.createPolicyDefinition(noConstraintPolicy());

            SOKRATES.createAsset("test-asset1");
            SOKRATES.createAsset("test-asset2");
            SOKRATES.createAsset("test-asset3");

            SOKRATES.createContractDefinition("test-asset1", "def1", noConstraintPolicyId, noConstraintPolicyId);
            SOKRATES.createContractDefinition("test-asset2", "def2", onlyPlatoId, noConstraintPolicyId);
            SOKRATES.createContractDefinition("test-asset3", "def3", onlyDiogenesId, noConstraintPolicyId);


            // act
            var catalog = PLATO.getCatalogDatasets(SOKRATES);
            assertThat(catalog).hasSize(2);
        }


        @Test
        @DisplayName("Verify that Plato receives only the offers he is permitted to (using the legacy BPN validation)")
        void requestCatalog_filteredByBpnLegacy_WithNamespace_shouldReject() {

            var onlyPlatoPolicy = bnpPolicy("BPN1", "BPN2", PLATO.getBpn());
            var onlyDiogenesPolicy = frameworkPolicy(Map.of(BUSINESS_PARTNER_LEGACY_EVALUATION_KEY, "ARISTOTELES-BPN"));

            var onlyPlatoId = SOKRATES.createPolicyDefinition(onlyPlatoPolicy);
            var onlyDiogenesId = SOKRATES.createPolicyDefinition(onlyDiogenesPolicy);
            var noConstraintPolicyId = SOKRATES.createPolicyDefinition(noConstraintPolicy());

            SOKRATES.createAsset("test-asset1");
            SOKRATES.createAsset("test-asset2");
            SOKRATES.createAsset("test-asset3");

            SOKRATES.createContractDefinition("test-asset1", "def1", noConstraintPolicyId, noConstraintPolicyId);
            SOKRATES.createContractDefinition("test-asset2", "def2", onlyPlatoId, noConstraintPolicyId);
            SOKRATES.createContractDefinition("test-asset3", "def3", onlyDiogenesId, noConstraintPolicyId);


            // act
            var catalog = PLATO.getCatalogDatasets(SOKRATES);
            assertThat(catalog).hasSize(2);
        }

        @Test
        @DisplayName("Verify that Plato receives only the offers he is permitted to (using the new BPN validation)")
        void requestCatalog_filteredByBpn_shouldReject() {

            var mustBeGreekPhilosopher = bpnGroupPolicy(Operator.IS_ANY_OF, "greek_customer", "philosopher");
            var mustBeGreekMathematician = bpnGroupPolicy(Operator.IS_ALL_OF, "greek_customer", "mathematician");


            SOKRATES.storeBusinessPartner(PLATO.getBpn(), "greek_customer", "philosopher");
            var philosopherId = SOKRATES.createPolicyDefinition(mustBeGreekPhilosopher);
            var mathId = SOKRATES.createPolicyDefinition(mustBeGreekMathematician);
            var noConstraintPolicyId = SOKRATES.createPolicyDefinition(noConstraintPolicy());

            SOKRATES.createAsset("test-asset1");
            SOKRATES.createAsset("test-asset2");
            SOKRATES.createAsset("test-asset3");

            SOKRATES.createContractDefinition("test-asset1", "def1", noConstraintPolicyId, noConstraintPolicyId);
            SOKRATES.createContractDefinition("test-asset2", "def2", philosopherId, noConstraintPolicyId);
            SOKRATES.createContractDefinition("test-asset3", "def3", mathId, noConstraintPolicyId);


            // act
            var catalog = PLATO.getCatalogDatasets(SOKRATES);
            assertThat(catalog).hasSize(2);
        }

        @Test
        @DisplayName("Multiple ContractDefinitions exist for one Asset")
        void requestCatalog_multipleOffersForAsset() {
            SOKRATES.storeBusinessPartner(PLATO.getBpn(), "test-group");
            SOKRATES.createAsset("asset-1");
            var noConstraintId = SOKRATES.createPolicyDefinition(noConstraintPolicy());
            var groupConstraintId = SOKRATES.createPolicyDefinition(bpnGroupPolicy(Operator.IS_ANY_OF, "test-group"));

            SOKRATES.createContractDefinition("asset-1", "def1", noConstraintId, noConstraintId);
            SOKRATES.createContractDefinition("asset-1", "def2", groupConstraintId, noConstraintId);

            var catalog = PLATO.getCatalogDatasets(SOKRATES);
            assertThat(catalog).hasSize(1)
                    .allSatisfy(cd -> {
                        assertThat(getDatasetAssetId(cd.asJsonObject())).isEqualTo("asset-1");
                        assertThat(getDatasetPolicies(cd)).hasSize(2);
                    });
        }

        @Test
        @DisplayName("Catalog with 1000 offers")
        void requestCatalog_of1000Assets_shouldContainAll() {
            var policy = bpnGroupPolicy(Operator.IS_NONE_OF, "test-group1", "test-group2");
            var policyId = SOKRATES.createPolicyDefinition(policy);
            var noConstraintId = SOKRATES.createPolicyDefinition(noConstraintPolicy());
            SOKRATES.storeBusinessPartner(PLATO.getBpn(), "test-group-3");

            range(0, 1000)
                    .forEach(i -> {
                        var assetId = "asset-" + i;
                        SOKRATES.createAsset(assetId);
                        SOKRATES.createContractDefinition(assetId, "def-" + i, policyId, noConstraintId);
                    });

            // request all at once
            var dataset = PLATO.getCatalogDatasets(SOKRATES, createQuery(1000, 0));
            assertThat(dataset).hasSize(1000);

            // request in chunks
            var o2 = PLATO.getCatalogDatasets(SOKRATES, createQuery(500, 0));
            var o3 = PLATO.getCatalogDatasets(SOKRATES, createQuery(500, 500));
            assertThat(o2).doesNotContainAnyElementsOf(o3);

        }
    }

    @Nested
    @EndToEndTest
    class InMemory extends Tests {

        @RegisterExtension
        protected static final ParticipantRuntime SOKRATES_RUNTIME = memoryRuntime(SOKRATES.getName(), SOKRATES.getBpn(), SOKRATES.getConfiguration());

        @RegisterExtension
        protected static final ParticipantRuntime PLATO_RUNTIME = memoryRuntime(PLATO.getName(), PLATO.getBpn(), PLATO.getConfiguration());

    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {

        @RegisterExtension
        protected static final PgParticipantRuntime SOKRATES_RUNTIME = pgRuntime(SOKRATES.getName(), SOKRATES.getBpn(), SOKRATES.getConfiguration());

        @RegisterExtension
        protected static final PgParticipantRuntime PLATO_RUNTIME = pgRuntime(PLATO.getName(), PLATO.getBpn(), PLATO.getConfiguration());

    }

}
