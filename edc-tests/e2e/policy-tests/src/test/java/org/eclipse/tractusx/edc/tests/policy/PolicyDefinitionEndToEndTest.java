/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.tests.policy;

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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.CX_POLICY_2025_09_NS;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_DID;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_NAME;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_DID;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_NAME;
import static org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions.emptyPolicy;
import static org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions.frameworkPermission;
import static org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions.frameworkPolicy;
import static org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions.inForceDateUsagePolicy;
import static org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions.policyFromRules;
import static org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions.policyWithEmptyRule;
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

    @DisplayName("Policy is accepted")
    @ParameterizedTest(name = "{1}")
    @ArgumentsSource(InValidNamespaceContractPolicyProvider.class)
    void shouldNotAcceptInvalidNamespacePolicyDefinitions(JsonObject policy, String description) {
        assertThatThrownBy(() -> PROVIDER.createPolicyDefinition(policy));
    }

    @DisplayName("Policy is not accepted")
    @ParameterizedTest(name = "{1}")
    @ArgumentsSource(InValidContractPolicyProvider.class)
    void shouldNotAcceptInvalidPolicyDefinitions(JsonObject policy, String description) {
        assertThatThrownBy(() -> PROVIDER.createPolicyDefinition(policy));
    }

    private abstract static class BaseContractPolicyProvider implements ArgumentsProvider {

        protected final String namespace;

        private BaseContractPolicyProvider(String namespace) {
            this.namespace = namespace;
        }

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Stream.of(
                    Arguments.of(frameworkPolicy(Map.of(namespace + "Membership", "active"), CX_POLICY_2025_09_NS + "access"), "MembershipCredential"),
                    Arguments.of(frameworkPolicy(Map.of(namespace + "FrameworkAgreement", "DataExchangeGovernance:1.0"), "use"), "DataExchangeGovernance use case"),
                    Arguments.of(frameworkPolicy(namespace + "AffiliatesRegion", Operator.IS_ANY_OF, List.of("cx.region.all:1", "cx.region.europe:1", "cx.region.northAmerica:1"), "use", true), "Affiliates Region"),
                    Arguments.of(frameworkPolicy(namespace + "AffiliatesRegion", Operator.IS_ANY_OF, List.of("cx.region.europe:1"), "use", true), "Affiliates Region (IS_ANY_OF, one element)"),
                    Arguments.of(frameworkPolicy(namespace + "AffiliatesBpnl", Operator.IS_ANY_OF, "BPNL00000000001A", "use", true), "Affiliates BPNL"),
                    Arguments.of(frameworkPolicy(namespace + "DataFrequency", Operator.EQ, "cx.dataFrequency.once:1", "use"), "Data Frequency"),
                    Arguments.of(frameworkPolicy(namespace + "DataUsageEndDate", Operator.EQ, "2025-06-30T14:30:00Z", "use"), "Data Usage End Date"),
                    Arguments.of(frameworkPolicy(namespace + "DataUsageEndDefinition", Operator.EQ, "cx.dataUsageEnd.unlimited:1", "use"), "Data Usage End Date Definition"),
                    Arguments.of(frameworkPolicy(namespace + "DataUsageEndDurationDays", Operator.EQ, 3, "use"), "Data Usage End Duration Days"),
                    Arguments.of(frameworkPolicy(namespace + "JurisdictionLocation", Operator.EQ, "test location", "use"), "Jurisdiction Location"),
                    Arguments.of(frameworkPolicy(namespace + "JurisdictionLocationReference", Operator.EQ, "cx.location.dataConsumer:1", "use"), "Jurisdiction Location Reference"),
                    Arguments.of(frameworkPolicy(namespace + "Liability", Operator.EQ, "cx.grossNegligence:1", "use"), "Liability"),
                    Arguments.of(frameworkPolicy(namespace + "Liability", Operator.EQ, "cx.slightNegligence:1", "use"), "Liability"),
                    Arguments.of(frameworkPolicy(namespace + "ManagedLegalEntityRegion", Operator.IS_ANY_OF, List.of("cx.region.all:1", "cx.region.europe:1"), "use", true), "Managed Legal Entity Region"),
                    Arguments.of(frameworkPolicy(namespace + "ManagedLegalEntityBpnl", Operator.IS_ANY_OF, "BPNL00000000001A", "use", true), "Managed Legal Entity BPNL"),
                    Arguments.of(frameworkPolicy(namespace + "Precedence", Operator.EQ, "cx.precedence.contractReference:1", "use"), "Precedence"),
                    Arguments.of(frameworkPolicy(namespace + "UsagePurpose", Operator.IS_ANY_OF, List.of("cx.core.legalRequirementForThirdparty:1", "cx.core.industrycore:1"), "use", true), "Usage Purpose"),
                    Arguments.of(frameworkPolicy(namespace + "VersionChanges", Operator.EQ, "cx.versionChanges.minor:1", "use"), "Version Changes"),
                    Arguments.of(frameworkPolicy(namespace + "Warranty", Operator.EQ, "cx.warranty.none:1", "use"), "Warranty"),
                    Arguments.of(frameworkPolicy(namespace + "WarrantyDefinition", Operator.EQ, "cx.warranty.contractEndDate:1", "use"), "Warranty Definition"),
                    Arguments.of(frameworkPolicy(namespace + "WarrantyDurationMonths", Operator.EQ, 3, "use"), "Warranty Duration Months"),
                    Arguments.of(frameworkPolicy(namespace + "ExclusiveUsage", Operator.EQ, "cx.exclusiveUsage.dataConsumer:1", "use"), "Exclusive Usage"),
                    Arguments.of(frameworkPolicy(namespace + "ContractReference", Operator.IS_ALL_OF, "contractReference", "use"), "Contract reference"),
                    Arguments.of(frameworkPolicy(namespace + "ContractTermination", Operator.EQ, "cx.data.deletion:1", "use"), "ContractTermination"),
                    Arguments.of(frameworkPolicy(namespace + "ConfidentialInformationMeasures", Operator.EQ, "cx.confidentiality.measures:1", "use"), "Confidential Information Measures"),
                    Arguments.of(frameworkPolicy(namespace + "ConfidentialInformationSharing", Operator.IS_ANY_OF, List.of("cx.sharing.affiliates:1"), "use", true), "Confidential Information Sharing"),
                    Arguments.of(frameworkPolicy(namespace + "BusinessPartnerGroup", Operator.IS_ANY_OF, "Some-group", "use", true), "Business Partner Group"),
                    Arguments.of(frameworkPolicy(namespace + "BusinessPartnerNumber", Operator.IS_ANY_OF, List.of("BPNL00000000001A"), "use", true), "Business Partner Number")
            );
        }
    }

    private static class ValidContractPolicyProvider extends BaseContractPolicyProvider {

        private ValidContractPolicyProvider() {
            super(CX_POLICY_2025_09_NS);
        }

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Stream.concat(super.provideArguments(extensionContext), Stream.of(
                    Arguments.of(emptyPolicy(), "Empty Policy"),
                    Arguments.of(policyWithEmptyRule(this.namespace + "access"), "Access policy with empty permission"),
                    Arguments.of(inForceDateUsagePolicy("gteq", "contractAgreement+0s", "lteq", "contractAgreement+10s"), "In force date policy")
            ));
        }
    }

    private static class InValidNamespaceContractPolicyProvider extends BaseContractPolicyProvider {

        private InValidNamespaceContractPolicyProvider() {
            super("");
        }
    }

    private static class InValidContractPolicyProvider extends BaseContractPolicyProvider {

        private InValidContractPolicyProvider() {
            super(CX_POLICY_2025_09_NS);
        }

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Stream.of(
                    Arguments.of(policyWithEmptyRule("use"), "Usage policy with empty permission"),
                    Arguments.of(policyFromRules("permission",
                            frameworkPermission(Map.of(namespace + "Membership", "active"), namespace + "access"),
                            frameworkPermission(Map.of(namespace + "UsagePurpose", "cx.core.industrycore:1"), "use")), "Policy with different actions types"),
                    Arguments.of(policyFromRules("permission",
                            frameworkPermission(Map.of(namespace + "Membership", "active"), "unknown-action")), "Policy with unknown actions types"),
                    Arguments.of(policyFromRules("prohibition",
                            frameworkPermission(Map.of(namespace + "Membership", "active"), namespace + "access")), "Access Policy with prohibition rule"),
                    Arguments.of(policyFromRules("permission",
                            frameworkPermission(Map.of(namespace + "UsagePurpose", "cx.core.industrycore:1"), namespace + "access")), "Access policy permission with not allowed constraints"),
                    Arguments.of(policyFromRules("permission",
                            frameworkPermission(Map.of(namespace + "BusinessPartnerNumber", "BPN0022232"), "use")), "Usage policy permission with not allowed constraints"),
                    Arguments.of(policyFromRules("prohibition",
                            frameworkPermission(Map.of(namespace + "AffiliatesRegion", "cx.region.europe:1"), "use")), "Usage policy prohibition with not allowed constraints"),
                    Arguments.of(policyFromRules("obligation",
                            frameworkPermission(Map.of(namespace + "UsagePurpose", "cx.core.industrycore:1"), "use")), "Usage policy obligation with not allowed constraints"),
                    Arguments.of(policyFromRules("permission",
                            frameworkPolicy(namespace + "WarrantyDurationMonths", Operator.EQ, 3, "use"),
                            frameworkPolicy(namespace + "WarrantyDefinition", Operator.EQ, "cx.warranty.contractEndDate:1", "use")), "Policy with mutually exclusive constraints")
            );
        }
    }
}
