package org.eclipse.tractusx.edc.tests;


import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.PolicyType;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.tractusx.edc.token.TestIdentityService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.policy.PolicyHelperFunctions.businessPartnerNumberPolicy;

@EndToEndTest
public class CatalogTest extends MultiRuntimeTest {


    private static TestIdentityService sokratesIsMock;
    private static TestIdentityService platoIsMock;

    @BeforeAll
    static void setup() {
        platoIsMock = new TestIdentityService("PLATOBPN");
        sokratesIsMock = new TestIdentityService("SOKRATESBPN");

        sokrates.registerServiceMock(IdentityService.class, sokratesIsMock);
        plato.registerServiceMock(IdentityService.class, platoIsMock);
    }

    @Test
    @Disabled
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
        var bpnAccessPolicy = businessPartnerNumberPolicy("ap", "BPN1", "BPN2", "PLATOBPN");
        var noConstraintPolicyId = "no-constraint";

        sokrates.createPolicy(bpnAccessPolicy);
        sokrates.createPolicy(noConstraintPolicy(noConstraintPolicyId));

        sokrates.createAsset("test-asset1", Map.of("canSee", "true"));
        sokrates.createAsset("test-asset2", Map.of("canSee", "false"));

        sokrates.createContractDefinition("test-asset1", "def1", noConstraintPolicyId, noConstraintPolicyId, 60);
        sokrates.createContractDefinition("test-asset2", "def2", "ap", noConstraintPolicyId, 60);


        // act
        var catalog = plato.requestCatalog(sokrates);
        assertThat(catalog.getContractOffers()).hasSize(2);
    }

    private PolicyDefinition noConstraintPolicy(String id) {
        return PolicyDefinition.Builder.newInstance()
                .id(id)
                .policy(Policy.Builder.newInstance()
                        .permission(Permission.Builder.newInstance()
                                .action(Action.Builder.newInstance().type("USE").build())
                                .build())
                        .type(PolicyType.SET)
                        .build())
                .build();
    }
}

