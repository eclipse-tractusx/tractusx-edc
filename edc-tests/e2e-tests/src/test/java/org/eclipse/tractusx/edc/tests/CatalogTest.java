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


import org.eclipse.edc.api.query.QuerySpecDto;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.tractusx.edc.lifecycle.MultiRuntimeTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.policy.PolicyHelperFunctions.businessPartnerNumberPolicy;
import static org.eclipse.tractusx.edc.policy.PolicyHelperFunctions.noConstraintPolicy;

@EndToEndTest
public class CatalogTest extends MultiRuntimeTest {

    @Test
    void requestCatalog_fulfillsPolicy_shouldReturnOffer() {
        // arrange
        sokrates.createAsset("test-asset", Map.of("fooprop", "fooval"));
        var accessPolicy = noConstraintPolicy("test-ap1");
        var contractPolicy = noConstraintPolicy("test-cp1");
        sokrates.createPolicy(accessPolicy);
        sokrates.createPolicy(contractPolicy);
        sokrates.createContractDefinition("test-asset", "test-def", "test-ap1", "test-cp1", 60);

        // act
        var catalog = plato.requestCatalog(sokrates);

        // assert
        assertThat(catalog.getContractOffers()).isNotEmpty()
                .hasSize(1)
                .allSatisfy(co -> {
                    assertThat(co.getAsset().getId()).isEqualTo("test-asset");
                    assertThat(co.getProvider().toString()).isEqualTo(sokrates.idsId());
                    assertThat(co.getConsumer().toString()).isEqualTo(plato.idsId());
                });

    }

    @Test
    @DisplayName("Verify that Plato receives only the offers he is permitted to")
    void requestCatalog_filteredByBpn_shouldReject() {
        var onlyPlatoPolicy = businessPartnerNumberPolicy("ap", "BPN1", "BPN2", plato.getBpn());
        var onlyDiogenesPolicy = businessPartnerNumberPolicy("dp", "ARISTOTELES-BPN");
        var noConstraintPolicyId = "no-constraint";

        sokrates.createPolicy(onlyPlatoPolicy);
        sokrates.createPolicy(noConstraintPolicy(noConstraintPolicyId));

        sokrates.createAsset("test-asset1", Map.of("canSee", "true"));
        sokrates.createAsset("test-asset2", Map.of("canSee", "true"));
        sokrates.createAsset("test-asset3", Map.of("canSee", "false"));

        sokrates.createContractDefinition("test-asset1", "def1", noConstraintPolicyId, noConstraintPolicyId, 60);
        sokrates.createContractDefinition("test-asset2", "def2", onlyPlatoPolicy.getId(), noConstraintPolicyId, 60);
        sokrates.createContractDefinition("test-asset3", "def3", onlyDiogenesPolicy.getId(), noConstraintPolicyId, 60);


        // act
        var catalog = plato.requestCatalog(sokrates);
        assertThat(catalog.getContractOffers()).hasSize(2);
    }

    @Test
    @DisplayName("Multiple ContractDefinitions exist for one Asset")
    void requestCatalog_multipleOffersForAsset() {
        sokrates.createAsset("asset-1", Map.of("test-key", "test-val"));
        sokrates.createPolicy(noConstraintPolicy("policy-1"));
        sokrates.createPolicy(businessPartnerNumberPolicy("policy-2", plato.getBpn()));

        sokrates.createContractDefinition("asset-1", "def1", "policy-1", "policy-1", 60);
        sokrates.createContractDefinition("asset-1", "def2", "policy-2", "policy-1", 60);

        var catalog = plato.requestCatalog(sokrates);
        assertThat(catalog.getContractOffers()).hasSize(2)
                .allSatisfy(cd -> assertThat(cd.getAsset().getId()).isEqualTo("asset-1"))
                // .hasToString is advisable as it handles NPEs better:
                .allSatisfy(cd -> assertThat(cd.getConsumer()).hasToString(plato.idsId()))
                .allSatisfy(cd -> assertThat(cd.getProvider()).hasToString(sokrates.idsId()));
    }

    @Test
    @DisplayName("Catalog with 1000 offers")
    void requestCatalog_of1000Assets_shouldContainAll() {
        var policy = businessPartnerNumberPolicy("policy-1", plato.getBpn());
        sokrates.createPolicy(policy);
        sokrates.createPolicy(noConstraintPolicy("noconstraint"));

        range(0, 1000)
                .forEach(i -> {
                    var assetId = "asset-" + i;
                    sokrates.createAsset(assetId, Map.of());
                    sokrates.createContractDefinition(assetId, "def-" + i, policy.getId(), "noconstraint", 60);
                });

        // request all at once
        var o = plato.requestCatalog(sokrates, QuerySpecDto.Builder.newInstance().limit(1000).offset(0).build()).getContractOffers();
        assertThat(o).hasSize(1000);

        // request in chunks
        var o2 = plato.requestCatalog(sokrates, QuerySpecDto.Builder.newInstance().limit(500).offset(0).build()).getContractOffers();
        var o3 = plato.requestCatalog(sokrates, QuerySpecDto.Builder.newInstance().limit(500).offset(500).build()).getContractOffers();
        assertThat(o2).doesNotContainAnyElementsOf(o3);

    }


}

