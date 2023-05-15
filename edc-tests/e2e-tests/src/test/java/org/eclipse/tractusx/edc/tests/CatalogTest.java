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

package org.eclipse.tractusx.edc.tests;


import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.tractusx.edc.lifecycle.MultiRuntimeTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.helpers.CatalogHelperFunctions.getDatasetAssetId;
import static org.eclipse.tractusx.edc.helpers.CatalogHelperFunctions.getDatasetPolicies;
import static org.eclipse.tractusx.edc.helpers.PolicyHelperFunctions.businessPartnerNumberPolicy;
import static org.eclipse.tractusx.edc.helpers.PolicyHelperFunctions.noConstraintPolicyDefinition;
import static org.eclipse.tractusx.edc.helpers.QueryHelperFunctions.createQuery;

@EndToEndTest
public class CatalogTest extends MultiRuntimeTest {

    @Test
    void requestCatalog_fulfillsPolicy_shouldReturnOffer() {
        // arrange
        sokrates.createAsset("test-asset");
        var accessPolicy = noConstraintPolicyDefinition("test-ap1");
        var contractPolicy = noConstraintPolicyDefinition("test-cp1");
        sokrates.createPolicy(accessPolicy);
        sokrates.createPolicy(contractPolicy);
        sokrates.createContractDefinition("test-asset", "test-def", "test-ap1", "test-cp1");

        // act
        var catalog = plato.getCatalogDatasets(sokrates);

        // assert
        assertThat(catalog).isNotEmpty()
                .hasSize(1)
                .allSatisfy(co -> {
                    assertThat(getDatasetAssetId(co)).isEqualTo("test-asset");
                });

    }

    @Test
    @DisplayName("Verify that Plato receives only the offers he is permitted to")
    void requestCatalog_filteredByBpn_shouldReject() {
        var onlyPlatoId = "ap";
        var onlyDiogenesId = "db";

        var onlyPlatoPolicy = businessPartnerNumberPolicy(onlyPlatoId, "BPN1", "BPN2", plato.getBpn());
        var onlyDiogenesPolicy = businessPartnerNumberPolicy(onlyDiogenesId, "ARISTOTELES-BPN");
        var noConstraintPolicyId = "no-constraint";

        sokrates.createPolicy(onlyPlatoPolicy);
        sokrates.createPolicy(onlyDiogenesPolicy);
        sokrates.createPolicy(noConstraintPolicyDefinition(noConstraintPolicyId));

        sokrates.createAsset("test-asset1");
        sokrates.createAsset("test-asset2");
        sokrates.createAsset("test-asset3");

        sokrates.createContractDefinition("test-asset1", "def1", noConstraintPolicyId, noConstraintPolicyId);
        sokrates.createContractDefinition("test-asset2", "def2", onlyPlatoId, noConstraintPolicyId);
        sokrates.createContractDefinition("test-asset3", "def3", onlyDiogenesId, noConstraintPolicyId);


        // act
        var catalog = plato.getCatalogDatasets(sokrates);
        assertThat(catalog).hasSize(2);
    }

    @Test
    @DisplayName("Multiple ContractDefinitions exist for one Asset")
    void requestCatalog_multipleOffersForAsset() {
        sokrates.createAsset("asset-1");
        sokrates.createPolicy(noConstraintPolicyDefinition("policy-1"));
        sokrates.createPolicy(businessPartnerNumberPolicy("policy-2", plato.getBpn()));

        sokrates.createContractDefinition("asset-1", "def1", "policy-1", "policy-1");
        sokrates.createContractDefinition("asset-1", "def2", "policy-2", "policy-1");

        var catalog = plato.getCatalogDatasets(sokrates);
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
        var policy = businessPartnerNumberPolicy(policyId, plato.getBpn());
        sokrates.createPolicy(policy);
        sokrates.createPolicy(noConstraintPolicyDefinition("noconstraint"));

        range(0, 1000)
                .forEach(i -> {
                    var assetId = "asset-" + i;
                    sokrates.createAsset(assetId);
                    sokrates.createContractDefinition(assetId, "def-" + i, policyId, "noconstraint");
                });

        // request all at once
        var dataset = plato.getCatalogDatasets(sokrates, createQuery(1000, 0));
        assertThat(dataset).hasSize(1000);

        // request in chunks
        var o2 = plato.getCatalogDatasets(sokrates, createQuery(500, 0));
        var o3 = plato.getCatalogDatasets(sokrates, createQuery(500, 500));
        assertThat(o2).doesNotContainAnyElementsOf(o3);

    }


}

