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

package org.eclipse.tractusx.edc.tests.catalog;

import org.eclipse.tractusx.edc.lifecycle.ParticipantRuntime;
import org.eclipse.tractusx.edc.lifecycle.tx.TxParticipant;
import org.eclipse.tractusx.edc.tag.MiwIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.test.system.utils.PolicyFixtures.noConstraintPolicy;
import static org.eclipse.tractusx.edc.helpers.CatalogHelperFunctions.getDatasetAssetId;
import static org.eclipse.tractusx.edc.helpers.PolicyHelperFunctions.TX_NAMESPACE;
import static org.eclipse.tractusx.edc.helpers.PolicyHelperFunctions.frameworkPolicy;
import static org.eclipse.tractusx.edc.helpers.PolicyHelperFunctions.frameworkTemplatePolicy;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_BPN;
import static org.eclipse.tractusx.edc.lifecycle.TestRuntimeConfiguration.SOKRATES_NAME;

@MiwIntegrationTest
public class MiwSsiCatalogTest {

    protected static final TxParticipant SOKRATES = TxParticipant.Builder.newInstance()
            .name(SOKRATES_NAME)
            .id(SOKRATES_BPN)
            .build();
    static final String MIW_SOKRATES_URL = "http://localhost:8000";
    static final String OAUTH_TOKEN_URL = "http://localhost:8080/realms/miw_test/protocol/openid-connect/token";

    @RegisterExtension
    protected static final ParticipantRuntime SOKRATES_RUNTIME = new ParticipantRuntime(
            ":edc-tests:runtime:runtime-memory-ssi",
            SOKRATES.getName(),
            SOKRATES.getBpn(),
            sokratesSsiMiwConfiguration()
    );

    public static Map<String, String> sokratesSsiMiwConfiguration() {
        var ssiConfiguration = new HashMap<String, String>() {
            {
                put("tx.ssi.miw.url", MIW_SOKRATES_URL);
                put("tx.ssi.oauth.token.url", OAUTH_TOKEN_URL);
                put("tx.ssi.oauth.client.id", "miw_private_client");
                put("tx.ssi.oauth.client.secret.alias", "client_secret_alias");
                put("tx.ssi.miw.authority.id", "BPNL000000000000");
                put("tx.ssi.miw.authority.issuer", "did:web:localhost%3A8000:BPNL000000000000");
                put("tx.vault.seed.secrets", "client_secret_alias:miw_private_client");
                put("tx.ssi.endpoint.audience", SOKRATES.protocolEndpoint().toString());
            }
        };
        var baseConfiguration = SOKRATES.getConfiguration();
        ssiConfiguration.putAll(baseConfiguration);
        return ssiConfiguration;
    }

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


    @Test
    @DisplayName("Verify that Sokrates receives only the offers he is permitted to using @context")
    void requestCatalog_fulfillsPolicy_shouldReturnOffer_withContexts() {
        // arrange
        SOKRATES.createAsset("test-asset");
        SOKRATES.createAsset("test-asset-1");

        var bpnAccessPolicy = frameworkTemplatePolicy("test-ap1", "BPN");
        var contractPolicy = noConstraintPolicy();
        var dismantlerAccessPolicy = frameworkTemplatePolicy("test-ap2", "Dismantler");

        SOKRATES.createPolicy(bpnAccessPolicy);
        var contractPolicyId = SOKRATES.createPolicyDefinition(contractPolicy);
        SOKRATES.createPolicy(dismantlerAccessPolicy);

        SOKRATES.createContractDefinition("test-asset", "test-def", "test-ap1", contractPolicyId);
        SOKRATES.createContractDefinition("test-asset-1", "test-def-2", "test-ap2", contractPolicyId);
        
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
