/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 * Copyright (c) 2025 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 * Copyright (c) 2025 Cofinity-X GmbH
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

package org.eclipse.tractusx.edc.tests.policy;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.tractusx.edc.tests.participant.TransferParticipant;
import org.eclipse.tractusx.edc.tests.runtimes.PostgresExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.tractusx.edc.jsonld.JsonLdExtension.CX_ODRL_CONTEXT;
import static org.eclipse.tractusx.edc.jsonld.JsonLdExtension.CX_POLICY_2025_09_CONTEXT;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_DID;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_NAME;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_DID;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_NAME;
import static org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions.emptyPolicy;
import static org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions.frameworkConstraint;
import static org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions.inForceDateUsagePolicy;
import static org.eclipse.tractusx.edc.tests.runtimes.Runtimes.pgRuntime;

@EndToEndTest
public class PolicyDefinitionEndToEndTest {
    private static final TransferParticipant CONSUMER = TransferParticipant.Builder.newInstance()
            .name(CONSUMER_NAME)
            .id(CONSUMER_DID)
            .bpn(CONSUMER_BPN)
            .build();


    private static final TransferParticipant PROVIDER = TransferParticipant.Builder.newInstance()
            .name(PROVIDER_NAME)
            .id(PROVIDER_DID)
            .bpn(PROVIDER_BPN)
            .build();

    @RegisterExtension
    @Order(0)
    private static final PostgresExtension POSTGRES = new PostgresExtension(CONSUMER.getName(), PROVIDER.getName());

    @RegisterExtension
    private static final RuntimeExtension CONSUMER_RUNTIME = pgRuntime(CONSUMER, POSTGRES);

    @RegisterExtension
    private static final RuntimeExtension PROVIDER_RUNTIME = pgRuntime(PROVIDER, POSTGRES);

    @DisplayName("Policy is accepted")
    @ParameterizedTest(name = "{1}")
    @ArgumentsSource(ValidContractPolicyProvider.class)
    void shouldAcceptValidPolicyDefinitions(JsonObject policy, String description) {
        PROVIDER.createPolicyDefinition(policy);
    }

    @DisplayName("Policy is not accepted due to missing context")
    @ParameterizedTest(name = "{1}")
    @ArgumentsSource(InValidNamespaceContractPolicyProvider.class)
    void shouldNotAcceptInvalidNamespacePolicyDefinitions(JsonObject policy, String description) {
        checkForValidationFailure(policy);
    }

    @DisplayName("Policy is not accepted because definition is not correct")
    @ParameterizedTest(name = "{1}")
    @ArgumentsSource(InValidContractPolicyProvider.class)
    void shouldNotAcceptInvalidPolicyDefinitions(JsonObject policy, String description) {
        checkForValidationFailure(policy);
    }

    private void checkForValidationFailure(JsonObject policy) {
        var response = createPolicyDefinition(policy);
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body().jsonPath().getString("[0].type")).isEqualTo("ValidationFailure");
    }

    private abstract static class BasePolicyProvider implements ArgumentsProvider {

        protected final String namespace;

        private BasePolicyProvider(String namespace) {
            this.namespace = namespace;
        }

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Stream.of(
                    Arguments.of(policyFromRules("permission", namespace, frameworkConstraint(Map.of("Membership", "active"), "use", Operator.EQ, false)), "MembershipCredential"),
                    Arguments.of(policyFromRules("permission", namespace, frameworkConstraint(Map.of("FrameworkAgreement", "DataExchangeGovernance:1.0"), "use", Operator.EQ, false)), "DataExchangeGovernance use case"),
                    Arguments.of(policyFromRules("permission", namespace, frameworkConstraint(Map.of("AffiliatesRegion", List.of("cx.region.all:1", "cx.region.europe:1", "cx.region.northAmerica:1")), "use", Operator.IS_ANY_OF, true)), "Affiliates Region"),
                    Arguments.of(policyFromRules("permission", namespace, frameworkConstraint(Map.of( "AffiliatesRegion", List.of("cx.region.europe:1")), "use", Operator.IS_ANY_OF, true)), "Affiliates Region (IS_ANY_OF, one element)"),
                    Arguments.of(policyFromRules("permission", namespace, frameworkConstraint(Map.of("AffiliatesBpnl", "BPNL00000000001A"), "use", Operator.IS_ANY_OF, false)), "Affiliates BPNL"),
                    Arguments.of(policyFromRules("permission", namespace, frameworkConstraint(Map.of("DataFrequency", "cx.dataFrequency.once:1"), "use", Operator.EQ, false)), "Data Frequency"),
                    Arguments.of(policyFromRules("permission", namespace, frameworkConstraint(Map.of("DataUsageEndDate", "2025-06-30T14:30:00Z"), "use", Operator.EQ, false)), "Data Usage End Date"),
                    Arguments.of(policyFromRules("permission", namespace, frameworkConstraint(Map.of("DataUsageEndDefinition", "cx.dataUsageEnd.unlimited:1"), "use",Operator.EQ, false)), "Data Usage End Date Definition"),
                    Arguments.of(policyFromRules("permission", namespace, frameworkConstraint(Map.of("DataUsageEndDurationDays", 3), "use", Operator.EQ, false)), "Data Usage End Duration Days"),
                    Arguments.of(policyFromRules("permission", namespace, frameworkConstraint(Map.of("JurisdictionLocation", "test location"), "use", Operator.EQ, false)), "Jurisdiction Location"),
                    Arguments.of(policyFromRules("permission", namespace, frameworkConstraint(Map.of("JurisdictionLocationReference", "cx.location.dataConsumer:1"), "use", Operator.EQ, false)), "Jurisdiction Location Reference"),
                    Arguments.of(policyFromRules("permission", namespace, frameworkConstraint(Map.of("Liability", "cx.grossNegligence:1"), "use", Operator.EQ, false)), "Liability"),
                    Arguments.of(policyFromRules("permission", namespace, frameworkConstraint(Map.of("Liability", "cx.slightNegligence:1"), "use", Operator.EQ, false)), "Liability"),
                    Arguments.of(policyFromRules("permission", namespace, frameworkConstraint(Map.of("Precedence", "cx.precedence.contractReference:1"), "use", Operator.EQ, false)), "Precedence"),
                    Arguments.of(policyFromRules("permission", namespace, frameworkConstraint(Map.of("UsagePurpose", List.of("cx.core.legalRequirementForThirdparty:1", "cx.core.industrycore:1")), "use", Operator.IS_ANY_OF, true)), "Usage Purpose"),
                    Arguments.of(policyFromRules("permission", namespace, frameworkConstraint(Map.of("VersionChanges", "cx.versionChanges.minor:1"), "use", Operator.EQ, false)), "Version Changes"),
                    Arguments.of(policyFromRules("permission", namespace, frameworkConstraint(Map.of("Warranty", "cx.warranty.none:1"), "use", Operator.EQ, false)), "Warranty"),
                    Arguments.of(policyFromRules("permission", namespace, frameworkConstraint(Map.of("WarrantyDefinition", "cx.warranty.contractEndDate:1"), "use", Operator.EQ, false)), "Warranty Definition"),
                    Arguments.of(policyFromRules("permission", namespace, frameworkConstraint(Map.of("WarrantyDurationMonths", 3), "use", Operator.EQ, false)), "Warranty Duration Months"),
                    Arguments.of(policyFromRules("permission", namespace, frameworkConstraint(Map.of("ExclusiveUsage", "cx.exclusiveUsage.dataConsumer:1"), "use", Operator.EQ, false)), "Exclusive Usage"),
                    Arguments.of(policyFromRules("permission", namespace, frameworkConstraint(Map.of("ContractReference", List.of("contractReference")), "use", Operator.IS_ALL_OF, true)), "Contract reference"),
                    Arguments.of(policyFromRules("permission", namespace, frameworkConstraint(Map.of("ContractTermination", "cx.data.deletion:1"), "use", Operator.EQ, false)), "ContractTermination"),
                    Arguments.of(policyFromRules("permission", namespace, frameworkConstraint(Map.of("ConfidentialInformationMeasures", "cx.confidentiality.measures:1"), "use", Operator.EQ, false)), "Confidential Information Measures"),
                    Arguments.of(policyFromRules("permission", namespace, frameworkConstraint(Map.of("ConfidentialInformationSharing", List.of("cx.sharing.affiliates:1")), "use", Operator.IS_ANY_OF, true)), "Confidential Information Sharing"),
                    Arguments.of(policyFromRules("permission", namespace, frameworkConstraint(Map.of("BusinessPartnerGroup", List.of("Some-group")), "access", Operator.IS_ANY_OF, true)), "Business Partner Group"),
                    Arguments.of(policyFromRules("permission", namespace, frameworkConstraint(Map.of("BusinessPartnerNumber", List.of("BPNL00000000001A")), "access", Operator.IS_ANY_OF, true)), "Business Partner Number")
            );
        }
    }

    private static class ValidContractPolicyProvider extends BasePolicyProvider {

        private ValidContractPolicyProvider() {
            super(CX_POLICY_2025_09_CONTEXT);
        }

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Stream.concat(super.provideArguments(extensionContext), Stream.of(
                    Arguments.of(emptyPolicy(), "Empty Policy"),
                    Arguments.of(policyWithEmptyRule( "access", this.namespace), "Access policy with empty permission"),
                    Arguments.of(inForceDateUsagePolicy("gteq", "contractAgreement+0s", "lteq", "contractAgreement+10s"), "In force date policy")
            ));
        }
    }

    private static class InValidNamespaceContractPolicyProvider extends BasePolicyProvider {

        private InValidNamespaceContractPolicyProvider() {
            super("");
        }
    }

    private static class InValidContractPolicyProvider extends BasePolicyProvider {

        private InValidContractPolicyProvider() {
            super(CX_POLICY_2025_09_CONTEXT);
        }

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Stream.of(
                    Arguments.of(policyWithEmptyRule("use", this.namespace), "Usage policy with empty permission"),
                    Arguments.of(policyFromRules("permission", namespace,
                            frameworkConstraint(Map.of("Membership", "active"), "access", Operator.EQ, false),
                            frameworkConstraint(Map.of( "UsagePurpose", List.of("cx.core.industrycore:1")), "use", Operator.IS_ANY_OF, true)), "Policy with different actions types"),
                    Arguments.of(policyFromRules("permission", namespace,
                            frameworkConstraint(Map.of("Membership", "active"), "unknown-action", Operator.EQ, false)), "Policy with unknown actions types"),
                    Arguments.of(policyFromRules("prohibition", namespace,
                            frameworkConstraint(Map.of( "Membership", "active"), "access", Operator.EQ, false)), "Access Policy with prohibition rule"),
                    Arguments.of(policyFromRules("permission", namespace,
                            frameworkConstraint(Map.of( "UsagePurpose", "cx.core.industrycore:1"), "access", Operator.EQ, false)), "Access policy permission with not allowed constraints"),
                    Arguments.of(policyFromRules("permission", namespace,
                            frameworkConstraint(Map.of( "BusinessPartnerNumber", "BPN0022232"), "use", Operator.EQ, false)), "Usage policy permission with not allowed constraints"),
                    Arguments.of(policyFromRules("prohibition", namespace,
                            frameworkConstraint(Map.of("AffiliatesRegion", "cx.region.europe:1"), "use", Operator.EQ, false)), "Usage policy prohibition with not allowed constraints"),
                    Arguments.of(policyFromRules("obligation", namespace,
                            frameworkConstraint(Map.of("UsagePurpose", "cx.core.industrycore:1"), "use", Operator.EQ, false)), "Usage policy obligation with not allowed constraints"),
                    Arguments.of(policyFromRules("permission", namespace,
                            frameworkConstraint(Map.of("WarrantyDurationMonths", 3), "use", Operator.EQ, false),
                            frameworkConstraint(Map.of("WarrantyDefinition", "cx.warranty.contractEndDate:1"), "use", Operator.EQ, false)), "Policy with mutually exclusive constraints")
            );
        }
    }

    private Response createPolicyDefinition(JsonObject policy) {
        JsonObject requestBody = Json.createObjectBuilder().add("@context", Json.createObjectBuilder().add("@vocab", "https://w3id.org/edc/v0.0.1/ns/")).add("@type", "PolicyDefinition").add("policy", policy).build();
        return (Response) PROVIDER.baseManagementRequest().contentType(ContentType.JSON).body(requestBody).when().post("/v3/policydefinitions", new Object[0]).then().extract();
    }

    private static JsonObject policyFromRules(String ruleType, String policyDefinition, JsonObject... rules) {
        var rulesArrayBuilder = Json.createArrayBuilder();
        for (JsonObject rule : rules) {
            rulesArrayBuilder.add(rule);
        }
        var contextArrayBuilder = Json.createArrayBuilder();
        contextArrayBuilder.add(CX_ODRL_CONTEXT);
        if (!policyDefinition.isBlank()) {
            contextArrayBuilder.add(policyDefinition);
        }

        return Json.createObjectBuilder()
                .add(CONTEXT, contextArrayBuilder)
                .add(TYPE, "Set")
                .add(ruleType, rulesArrayBuilder)
                .build();
    }

    private static JsonObject policyWithEmptyRule(String action, String policyContext) {
        var rule = Json.createObjectBuilder()
                .add("action", action)
                .build();
        var rulesArrayBuilder = Json.createArrayBuilder();
        rulesArrayBuilder.add(rule);
        var contextArrayBuilder = Json.createArrayBuilder();
        contextArrayBuilder.add(CX_ODRL_CONTEXT);
        contextArrayBuilder.add(policyContext);

        return Json.createObjectBuilder()
                .add(CONTEXT, contextArrayBuilder)
                .add(TYPE, "Set")
                .add("permission", rulesArrayBuilder)
                .build();
    }
}
