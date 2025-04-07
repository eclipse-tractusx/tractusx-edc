/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.tractusx.edc.tests.participant.TransferParticipant;
import org.eclipse.tractusx.edc.tests.runtimes.PostgresExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.inForceDatePolicy;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.CX_POLICY_NS;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_NAME;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_NAME;
import static org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions.frameworkPolicy;
import static org.eclipse.tractusx.edc.tests.runtimes.Runtimes.memoryRuntime;
import static org.eclipse.tractusx.edc.tests.runtimes.Runtimes.pgRuntime;

public class PolicyDefinitionEndToEndTest {

    protected static final TransferParticipant CONSUMER = TransferParticipant.Builder.newInstance()
            .name(CONSUMER_NAME)
            .id(CONSUMER_BPN)
            .build();


    protected static final TransferParticipant PROVIDER = TransferParticipant.Builder.newInstance()
            .name(PROVIDER_NAME)
            .id(PROVIDER_BPN)
            .build();

    abstract static class Tests {

        @DisplayName("Policy is accepted")
        @ParameterizedTest(name = "{1}")
        @ArgumentsSource(ValidContractPolicyProvider.class)
        void shouldAcceptValidPolicyDefinitions(JsonObject policy, String description) {
            PROVIDER.createPolicyDefinition(policy);
        }

        @DisplayName("Policy is accepted")
        @ParameterizedTest(name = "{1}")
        @ArgumentsSource(InValidContractPolicyProvider.class)
        void shouldNotAcceptInvalidValidPolicyDefinitions(JsonObject policy, String description) {
            assertThatThrownBy(() -> PROVIDER.createPolicyDefinition(policy));
        }

    }

    private abstract static class BaseContractPolicyProvider implements ArgumentsProvider {

        private final String namespace;

        private BaseContractPolicyProvider(String namespace) {
            this.namespace = namespace;
        }

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Stream.of(
                    Arguments.of(frameworkPolicy(Map.of(namespace + "Membership", "active")), "MembershipCredential"),
                    Arguments.of(frameworkPolicy(Map.of(namespace + "FrameworkAgreement.pcf", "active")), "PCF Use Case (legacy notation)"),
                    Arguments.of(frameworkPolicy(Map.of(namespace + "FrameworkAgreement", "Pcf")), "PCF Use Case (new notation)"),
                    Arguments.of(frameworkPolicy(Map.of(namespace + "FrameworkAgreement", "DataExchangeGovernance:1.0.0")), "DataExchangeGovernance use case"),
                    Arguments.of(frameworkPolicy(Map.of(namespace + "Dismantler", "active")), "Dismantler Credential"),
                    Arguments.of(frameworkPolicy(Map.of(namespace + "Dismantler.activityType", "vehicleDismantle")), "Dismantler Cred (activity type)"),
                    Arguments.of(frameworkPolicy(namespace + "Dismantler.allowedBrands", Operator.IS_ANY_OF, List.of("Moskvich", "Tatra")), "Dismantler allowedBrands (IS_ANY_OF, one intersects)"),
                    Arguments.of(frameworkPolicy(namespace + "Dismantler.allowedBrands", Operator.EQ, List.of("Moskvich", "Lada")), "Dismantler allowedBrands (EQ, exact match)"),
                    Arguments.of(frameworkPolicy(namespace + "Dismantler.allowedBrands", Operator.IS_NONE_OF, List.of("Yugo", "Tatra")), "Dismantler allowedBrands (IS_NONE_OF, no intersect)"),
                    Arguments.of(frameworkPolicy(namespace + "Dismantler.allowedBrands", Operator.IN, List.of("Moskvich", "Tatra", "Yugo", "Lada")), "Dismantler allowedBrands (IN, fully contained)"),
                    Arguments.of(frameworkPolicy(Map.of(namespace + "UsagePurpose", "cx.core.industrycore:1")), "Usage Purpose"),
                    Arguments.of(frameworkPolicy(Map.of(namespace + "ContractReference", "contractReference")), "Contract reference")
            );
        }
    }

    private static class ValidContractPolicyProvider extends BaseContractPolicyProvider {

        private ValidContractPolicyProvider() {
            super(CX_POLICY_NS);
        }

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Stream.concat(super.provideArguments(extensionContext), Stream.of(
                    Arguments.of(inForceDatePolicy("gteq", "contractAgreement+0s", "lteq", "contractAgreement+10s"), "In force date policy")
            ));
        }
    }

    private static class InValidContractPolicyProvider extends BaseContractPolicyProvider {

        private InValidContractPolicyProvider() {
            super("");
        }
    }

    @Nested
    @EndToEndTest
    class InMemory extends Tests {

        @RegisterExtension
        protected static final RuntimeExtension CONSUMER_RUNTIME = memoryRuntime(CONSUMER.getName(), CONSUMER.getBpn(), CONSUMER::getConfig);

        @RegisterExtension
        protected static final RuntimeExtension PROVIDER_RUNTIME = memoryRuntime(PROVIDER.getName(), PROVIDER.getBpn(), PROVIDER::getConfig);

    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {

        @RegisterExtension
        @Order(0)
        private static final PostgresExtension POSTGRES = new PostgresExtension(CONSUMER.getName(), PROVIDER.getName());

        @RegisterExtension
        protected static final RuntimeExtension CONSUMER_RUNTIME = pgRuntime(CONSUMER, POSTGRES);

        @RegisterExtension
        protected static final RuntimeExtension PROVIDER_RUNTIME = pgRuntime(PROVIDER, POSTGRES);

    }
}
