/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 * Copyright (c) 2025 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
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

import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.tractusx.edc.tests.participant.TransferParticipant;
import org.eclipse.tractusx.edc.tests.runtimes.PostgresExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;

import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.noConstraintPolicy;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.CX_POLICY_2025_09_NS;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_DID;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_NAME;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.DSP_2025;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.DSP_2025_PATH;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_DID;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_NAME;
import static org.eclipse.tractusx.edc.tests.helpers.CatalogHelperFunctions.getDatasetAssetId;
import static org.eclipse.tractusx.edc.tests.helpers.CatalogHelperFunctions.getDatasetPolicies;
import static org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions.BUSINESS_PARTNER_LEGACY_EVALUATION_KEY;
import static org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions.bpnGroupPolicy;
import static org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions.bpnPolicy;
import static org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions.frameworkPolicy;
import static org.eclipse.tractusx.edc.tests.helpers.QueryHelperFunctions.createQuery;
import static org.eclipse.tractusx.edc.tests.runtimes.Runtimes.pgRuntime;

@EndToEndTest
public class CatalogTest {

    private static final TransferParticipant CONSUMER = TransferParticipant.Builder.newInstance()
            .name(CONSUMER_NAME)
            .id(CONSUMER_DID)
            .bpn(CONSUMER_BPN)
            .protocol(DSP_2025)
            .protocolVersionPath(DSP_2025_PATH)
            .build();


    private static final TransferParticipant PROVIDER = TransferParticipant.Builder.newInstance()
            .name(PROVIDER_NAME)
            .id(PROVIDER_DID)
            .bpn(PROVIDER_BPN)
            .protocol(DSP_2025)
            .protocolVersionPath(DSP_2025_PATH)
            .build();

    @RegisterExtension
    @Order(0)
    private static final PostgresExtension POSTGRES = new PostgresExtension(CONSUMER.getName(), PROVIDER.getName());

    @RegisterExtension
    private static final RuntimeExtension CONSUMER_RUNTIME = pgRuntime(CONSUMER, POSTGRES);

    @RegisterExtension
    private static final RuntimeExtension PROVIDER_RUNTIME = pgRuntime(PROVIDER, POSTGRES);

    @BeforeEach
    void setup() {
        CONSUMER.setJsonLd(CONSUMER_RUNTIME.getService(JsonLd.class));
    }

    @Test
    @DisplayName("Consumer gets catalog from the provider. No constraints.")
    void requestCatalog_fulfillsPolicy_shouldReturnOffer() {
        // arrange
        PROVIDER.createAsset("test-asset");
        var ap = PROVIDER.createPolicyDefinition(noConstraintPolicy());
        var cp = PROVIDER.createPolicyDefinition(noConstraintPolicy());
        PROVIDER.createContractDefinition("test-asset", "test-def", ap, cp);

        // act
        var catalog = CONSUMER.getCatalogDatasets(PROVIDER);

        // assert
        assertThat(catalog).isNotEmpty()
                .hasSize(1)
                .allSatisfy(co -> {
                    assertThat(getDatasetAssetId(co.asJsonObject())).isEqualTo("test-asset");
                });

    }

    @Test
    @DisplayName("Verify that the consumer receives only the offers he is permitted to (using the legacy BPN validation)")
    void requestCatalog_filteredByBpnLegacy_shouldReject() {
        var onlyConsumerPolicy = bpnPolicy("BPNLAAAAAAAAAAAA", "BPNLAAAAAAAAAAAA", CONSUMER.getBpn());
        var onlyDiogenesPolicy = bpnPolicy("BPNL0ARISTOTELES");

        var onlyConsumerId = PROVIDER.createPolicyDefinition(onlyConsumerPolicy);
        var onlyDiogenesId = PROVIDER.createPolicyDefinition(onlyDiogenesPolicy);
        var noConstraintPolicyId = PROVIDER.createPolicyDefinition(noConstraintPolicy());

        PROVIDER.createAsset("test-asset1");
        PROVIDER.createAsset("test-asset2");
        PROVIDER.createAsset("test-asset3");

        PROVIDER.createContractDefinition("test-asset1", "def1", noConstraintPolicyId, noConstraintPolicyId);
        PROVIDER.createContractDefinition("test-asset2", "def2", onlyConsumerId, noConstraintPolicyId);
        PROVIDER.createContractDefinition("test-asset3", "def3", onlyDiogenesId, noConstraintPolicyId);


        // act
        var catalog = CONSUMER.getCatalogDatasets(PROVIDER);
        assertThat(catalog).hasSize(1);
    }


    @Test
    @DisplayName("Verify that the consumer receives only the offers he is permitted to (using the legacy BPN validation)")
    void requestCatalog_filteredByBpnLegacy_WithNamespace_shouldReject() {

        var onlyConsumerPolicy = bpnPolicy("BPNLAAAAAAAAAAAA", "BPNL123456ABCDEF", CONSUMER.getBpn());

        var onlyConsumerId = PROVIDER.createPolicyDefinition(onlyConsumerPolicy);
        var noConstraintPolicyId = PROVIDER.createPolicyDefinition(noConstraintPolicy());

        PROVIDER.createAsset("test-asset1");
        PROVIDER.createAsset("test-asset2");

        PROVIDER.createContractDefinition("test-asset1", "def1", noConstraintPolicyId, noConstraintPolicyId);
        PROVIDER.createContractDefinition("test-asset2", "def2", onlyConsumerId, noConstraintPolicyId);

        // act
        var catalog = CONSUMER.getCatalogDatasets(PROVIDER);
        assertThat(catalog).hasSize(1);
    }

    @Test
    @DisplayName("Verify that the consumer receives only the offers he is permitted to (using the new BPN validation)")
    void requestCatalog_filteredByBpn_shouldReject() {

        var mustBeGreekPhilosopher = bpnGroupPolicy(Operator.IS_ANY_OF, "greek_customer", "philosopher");

        PROVIDER.storeBusinessPartner(CONSUMER.getBpn(), "greek_customer", "philosopher");
        var philosopherId = PROVIDER.createPolicyDefinition(mustBeGreekPhilosopher);
        var noConstraintPolicyId = PROVIDER.createPolicyDefinition(noConstraintPolicy());

        PROVIDER.createAsset("test-asset1");
        PROVIDER.createAsset("test-asset2");

        PROVIDER.createContractDefinition("test-asset1", "def1", noConstraintPolicyId, noConstraintPolicyId);
        PROVIDER.createContractDefinition("test-asset2", "def2", philosopherId, noConstraintPolicyId);

        // act
        var catalog = CONSUMER.getCatalogDatasets(PROVIDER);
        assertThat(catalog).hasSize(2);
    }

    @Test
    @DisplayName("Multiple ContractDefinitions exist for one Asset")
    void requestCatalog_multipleOffersForAsset() {
        PROVIDER.storeBusinessPartner(CONSUMER.getBpn(), "test-group");
        PROVIDER.createAsset("asset-1");
        var noConstraintId = PROVIDER.createPolicyDefinition(noConstraintPolicy());
        var groupConstraintId = PROVIDER.createPolicyDefinition(bpnGroupPolicy(Operator.IS_ANY_OF, "test-group"));

        PROVIDER.createContractDefinition("asset-1", "def1", noConstraintId, noConstraintId);
        PROVIDER.createContractDefinition("asset-1", "def2", groupConstraintId, noConstraintId);

        var catalog = CONSUMER.getCatalogDatasets(PROVIDER);
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
        var policyId = PROVIDER.createPolicyDefinition(policy);
        var noConstraintId = PROVIDER.createPolicyDefinition(noConstraintPolicy());
        PROVIDER.storeBusinessPartner(CONSUMER.getBpn(), "test-group-3");

        range(0, 1000)
                .forEach(i -> {
                    var assetId = "asset-" + i;
                    PROVIDER.createAsset(assetId);
                    PROVIDER.createContractDefinition(assetId, "def-" + i, policyId, noConstraintId);
                });

        // request all at once
        var dataset = CONSUMER.getCatalogDatasets(PROVIDER, createQuery(1000, 0));
        assertThat(dataset).hasSize(1000);

        // request in chunks
        var o2 = CONSUMER.getCatalogDatasets(PROVIDER, createQuery(500, 0));
        var o3 = CONSUMER.getCatalogDatasets(PROVIDER, createQuery(500, 500));
        assertThat(o2).doesNotContainAnyElementsOf(o3);

    }

}
