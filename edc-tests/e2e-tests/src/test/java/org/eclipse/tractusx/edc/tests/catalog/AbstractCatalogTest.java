/*
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.tests.catalog;


import org.eclipse.edc.policy.model.Operator;
import org.eclipse.tractusx.edc.lifecycle.Participant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.helpers.CatalogHelperFunctions.getDatasetAssetId;
import static org.eclipse.tractusx.edc.helpers.CatalogHelperFunctions.getDatasetPolicies;
import static org.eclipse.tractusx.edc.helpers.PolicyHelperFunctions.BUSINESS_PARTNER_LEGACY_EVALUATION_KEY;
import static org.eclipse.tractusx.edc.helpers.PolicyHelperFunctions.businessPartnerGroupPolicy;
import static org.eclipse.tractusx.edc.helpers.PolicyHelperFunctions.businessPartnerNumberPolicy;
import static org.eclipse.tractusx.edc.helpers.PolicyHelperFunctions.frameworkPolicy;
import static org.eclipse.tractusx.edc.helpers.PolicyHelperFunctions.noConstraintPolicyDefinition;
import static org.eclipse.tractusx.edc.helpers.QueryHelperFunctions.createQuery;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_BPN;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.PLATO_NAME;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_BPN;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_NAME;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.platoConfiguration;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.sokratesConfiguration;

public abstract class AbstractCatalogTest {

    protected static final Participant SOKRATES = new Participant(SOKRATES_NAME, SOKRATES_BPN, sokratesConfiguration());
    protected static final Participant PLATO = new Participant(PLATO_NAME, PLATO_BPN, platoConfiguration());

    @Test
    @DisplayName("Plato gets catalog from Sokrates. No constraints.")
    void requestCatalog_fulfillsPolicy_shouldReturnOffer() {
        // arrange
        SOKRATES.createAsset("test-asset");
        var accessPolicy = noConstraintPolicyDefinition("test-ap1");
        var contractPolicy = noConstraintPolicyDefinition("test-cp1");
        SOKRATES.createPolicy(accessPolicy);
        SOKRATES.createPolicy(contractPolicy);
        SOKRATES.createContractDefinition("test-asset", "test-def", "test-ap1", "test-cp1");

        // act
        var catalog = PLATO.getCatalogDatasets(SOKRATES);

        // assert
        assertThat(catalog).isNotEmpty()
                .hasSize(1)
                .allSatisfy(co -> {
                    assertThat(getDatasetAssetId(co)).isEqualTo("test-asset");
                });

    }

    @Test
    @DisplayName("Verify that Plato receives only the offers he is permitted to (using the legacy BPN validation)")
    void requestCatalog_filteredByBpnLegacy_shouldReject() {
        var onlyPlatoId = "ap";
        var onlyDiogenesId = "db";

        var onlyPlatoPolicy = businessPartnerNumberPolicy(onlyPlatoId, "BPN1", "BPN2", PLATO.getBpn());
        var onlyDiogenesPolicy = businessPartnerNumberPolicy(onlyDiogenesId, "ARISTOTELES-BPN");
        var noConstraintPolicyId = "no-constraint";

        SOKRATES.createPolicy(onlyPlatoPolicy);
        SOKRATES.createPolicy(onlyDiogenesPolicy);
        SOKRATES.createPolicy(noConstraintPolicyDefinition(noConstraintPolicyId));

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
        var onlyPlatoId = "ap";
        var onlyDiogenesId = "db";

        var onlyPlatoPolicy = businessPartnerNumberPolicy(onlyPlatoId, "BPN1", "BPN2", PLATO.getBpn());
        var onlyDiogenesPolicy = frameworkPolicy(onlyDiogenesId, Map.of(BUSINESS_PARTNER_LEGACY_EVALUATION_KEY, "ARISTOTELES-BPN"));
        var noConstraintPolicyId = "no-constraint";

        SOKRATES.createPolicy(onlyPlatoPolicy);
        SOKRATES.createPolicy(onlyDiogenesPolicy);
        SOKRATES.createPolicy(noConstraintPolicyDefinition(noConstraintPolicyId));

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
        var philosopherId = "ap";
        var mathId = "db";

        var mustBeGreekPhilosopher = businessPartnerGroupPolicy(philosopherId, Operator.IS_ANY_OF, "greek_customer", "philosopher");
        var mustBeGreekMathematician = businessPartnerGroupPolicy(mathId, Operator.IS_ALL_OF, "greek_customer", "mathematician");
        var noConstraintPolicyId = "no-constraint";


        SOKRATES.storeBusinessPartner(PLATO.getBpn(), "greek_customer", "philosopher");
        SOKRATES.createPolicy(mustBeGreekPhilosopher);
        SOKRATES.createPolicy(mustBeGreekMathematician);
        SOKRATES.createPolicy(noConstraintPolicyDefinition(noConstraintPolicyId));

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
        SOKRATES.createPolicy(noConstraintPolicyDefinition("policy-1"));
        SOKRATES.createPolicy(businessPartnerGroupPolicy("policy-2", Operator.IS_ANY_OF, "test-group"));

        SOKRATES.createContractDefinition("asset-1", "def1", "policy-1", "policy-1");
        SOKRATES.createContractDefinition("asset-1", "def2", "policy-2", "policy-1");

        var catalog = PLATO.getCatalogDatasets(SOKRATES);
        assertThat(catalog).hasSize(1)
                .allSatisfy(cd -> {
                    assertThat(getDatasetAssetId(cd)).isEqualTo("asset-1");
                    assertThat(getDatasetPolicies(cd)).hasSize(2);
                });
    }

    @Test
    @DisplayName("Catalog with 1000 offers")
    void requestCatalog_of1000Assets_shouldContainAll() {
        var policyId = "policy-1";
        var policy = businessPartnerGroupPolicy(policyId, Operator.IS_NONE_OF, "test-group1", "test-group2");
        SOKRATES.createPolicy(policy);
        SOKRATES.createPolicy(noConstraintPolicyDefinition("noconstraint"));
        SOKRATES.storeBusinessPartner(PLATO.getBpn(), "test-group-3");

        range(0, 1000)
                .forEach(i -> {
                    var assetId = "asset-" + i;
                    SOKRATES.createAsset(assetId);
                    SOKRATES.createContractDefinition(assetId, "def-" + i, policyId, "noconstraint");
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

