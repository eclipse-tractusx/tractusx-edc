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

package org.eclipse.tractusx.edc.iam.iatp.scope;

import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreementMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationTerminationMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequestMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferTerminationMessage;
import org.eclipse.edc.policy.engine.spi.PolicyContextImpl;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.iam.RequestContext;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;
import org.eclipse.tractusx.edc.edr.spi.CoreConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.iam.iatp.TxIatpConstants.CREDENTIAL_TYPE_NAMESPACE;
import static org.eclipse.tractusx.edc.iam.iatp.scope.CredentialScopeExtractor.FRAMEWORK_CREDENTIAL_PREFIX;
import static org.mockito.Mockito.mock;

public class CredentialScopeExtractorTest {

    private final Monitor monitor = mock();
    private CredentialScopeExtractor extractor;

    @BeforeEach
    void setup() {
        extractor = new CredentialScopeExtractor(monitor);
    }

    @DisplayName("Scope extractor with supported messages")
    @ParameterizedTest(name = "{1}")
    @ArgumentsSource(SupportedMessages.class)
    void verify_extractScopes(RemoteMessage message) {
        var builder = TokenParameters.Builder.newInstance();
        var requestContext = RequestContext.Builder.newInstance().message(message).direction(RequestContext.Direction.Egress).build();
        var ctx = PolicyContextImpl.Builder.newInstance().additional(TokenParameters.Builder.class, builder).additional(RequestContext.class, requestContext).build();
        var scopes = extractor.extractScopes(CoreConstants.CX_POLICY_NS + FRAMEWORK_CREDENTIAL_PREFIX + ".pfc", null, null, ctx);
        assertThat(scopes).contains(CREDENTIAL_TYPE_NAMESPACE + ":PfcCredential:read");
    }


    @DisplayName("Scope extractor with not supported messages")
    @ParameterizedTest(name = "{1}")
    @ArgumentsSource(NotSupportedMessages.class)
    void verify_extractScopes_isEmpty_whenNotSupportedMessages(RemoteMessage message) {
        var builder = TokenParameters.Builder.newInstance();
        var requestContext = RequestContext.Builder.newInstance().message(message).direction(RequestContext.Direction.Egress).build();
        var ctx = PolicyContextImpl.Builder.newInstance().additional(TokenParameters.Builder.class, builder).additional(RequestContext.class, requestContext).build();
        var scopes = extractor.extractScopes(CoreConstants.CX_POLICY_NS + FRAMEWORK_CREDENTIAL_PREFIX + ".pfc", null, null, ctx);
        assertThat(scopes).isEmpty();
    }

    @Test
    void verify_extractScope_Empty() {
        var builder = TokenParameters.Builder.newInstance();
        var ctx = PolicyContextImpl.Builder.newInstance().additional(TokenParameters.Builder.class, builder).build();
        var scopes = extractor.extractScopes("wrong", null, null, ctx);
        assertThat(scopes).isEmpty();
    }

    private static class SupportedMessages implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            var offer = ContractOffer.Builder.newInstance().id("id").assetId("assetId").policy(Policy.Builder.newInstance().build()).build();
            return Stream.of(
                    Arguments.of(CatalogRequestMessage.Builder.newInstance().build()),
                    Arguments.of(ContractRequestMessage.Builder.newInstance().contractOffer(offer).callbackAddress("cb").build()),
                    Arguments.of(TransferRequestMessage.Builder.newInstance().callbackAddress("cb").build())
            );
        }
    }

    private static class NotSupportedMessages implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Stream.of(
                    Arguments.of(ContractNegotiationTerminationMessage.Builder.newInstance().build()),
                    Arguments.of(ContractAgreementMessage.Builder.newInstance().counterPartyAddress("cb").contractAgreement(mock()).build()),
                    Arguments.of(TransferTerminationMessage.Builder.newInstance().counterPartyAddress("pd").build())
            );
        }
    }
}
