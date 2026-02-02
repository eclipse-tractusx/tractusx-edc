/*
 * Copyright (c) 2025 Cofinity-X GmbH
 * Copyright (c) 2026 SAP SE
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
 */

package org.eclipse.tractusx.edc.protocol.cx.identifier;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.tractusx.edc.protocol.core.identifier.MembershipCredentialIdExtractionFunction;
import org.eclipse.tractusx.edc.protocol.core.identifier.MembershipCredentialIdExtractionFunctionTest;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.CX_CREDENTIAL_NS;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BpnExtractionFunctionTest extends MembershipCredentialIdExtractionFunctionTest {

    public static final String BPN = "bpn";
    public static final String ID_PROPERTY = "holderIdentifier";

    private final Monitor monitor = mock();

    @Override
    protected MembershipCredentialIdExtractionFunction extractionFunction() {
        when(monitor.withPrefix(anyString())).thenReturn(monitor);
        return new BpnExtractionFunction(monitor);
    }
    
    @Override
    protected String expectedId() {
        return BPN;
    }

    @ParameterizedTest
    @ArgumentsSource(VerifiableCredentialArgumentProvider.class)
    void apply(VerifiableCredential credential) {
        var id = extractionFunction().apply(ClaimToken.Builder.newInstance().claim("vc", List.of(credential)).build());
        assertThat(id).isEqualTo(expectedId());
    }

    private static class VerifiableCredentialArgumentProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of(vc("MembershipCredential", Map.of("id", DID, ID_PROPERTY, BPN))),
                    Arguments.of(vc(CX_CREDENTIAL_NS + "MembershipCredential", Map.of("id", DID, ID_PROPERTY, BPN))),
                    Arguments.of(vc(CX_CREDENTIAL_NS + "MembershipCredential", Map.of("id", DID, CX_CREDENTIAL_NS + ID_PROPERTY, BPN))));
        }
    }
}
