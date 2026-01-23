/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.protocol.lib;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.CX_CREDENTIAL_NS;

public abstract class MembershipCredentialIdExtractionFunctionTest {
    
    public static final String BPN = "bpn";
    public static final String DID = "did:web:example";

    @ParameterizedTest
    @ArgumentsSource(VerifiableCredentialArgumentProvider.class)
    void apply(VerifiableCredential credential) {
        var id = extractionFunction().apply(ClaimToken.Builder.newInstance().claim("vc", List.of(credential)).build());
        assertThat(id).isEqualTo(expectedId());
    }
    
    @Test
    void apply_fails_WhenCredentialNotFound() {
        var function = extractionFunction();
        assertThatThrownBy(() -> function.apply(ClaimToken.Builder.newInstance().claim("vc", List.of(vc("FooCredential", Map.of("foo", "bar")))).build()))
                .isInstanceOf(EdcException.class)
                .hasMessage("Required credential type 'MembershipCredential' not present in ClaimToken, cannot extract property '%s'", function.identityProperty());
    }
    
    @Test
    void apply_fails_whenNoVcClaims() {
        assertThatThrownBy(() -> extractionFunction().apply(ClaimToken.Builder.newInstance().build()))
                .isInstanceOf(EdcException.class)
                .hasMessageContaining("Failed to fetch credentials from the claim token: ClaimToken did not contain a 'vc' claim");
    }
    
    @Test
    void apply_fails_whenNullVcClaims() {
        
        assertThatThrownBy(() -> extractionFunction().apply(ClaimToken.Builder.newInstance().claim("vc", null).build()))
                .isInstanceOf(EdcException.class)
                .hasMessageContaining("Failed to fetch credentials from the claim token: ClaimToken did not contain a 'vc' claim");
    }
    
    @Test
    void apply_fails_WhenVcClaimIsNotList() {
        assertThatThrownBy(() -> extractionFunction().apply(ClaimToken.Builder.newInstance().claim("vc", "wrong").build()))
                .isInstanceOf(EdcException.class)
                .hasMessageContaining("Failed to fetch credentials from the claim token: ClaimToken contains a 'vc' claim, but the type is incorrect. Expected java.util.List, got java.lang.String.");
    }
    
    @Test
    void apply_fails_WhenVcClaimsIsEmptyList() {
        assertThatThrownBy(() -> extractionFunction().apply(ClaimToken.Builder.newInstance().claim("vc", List.of()).build()))
                .isInstanceOf(EdcException.class)
                .hasMessageContaining("Failed to fetch credentials from the claim token: ClaimToken contains a 'vc' claim but it did not contain any VerifiableCredentials.");
    }
    
    private static class VerifiableCredentialArgumentProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of(vc("MembershipCredential", Map.of("id", DID, "holderIdentifier", BPN))),
                    Arguments.of(vc(CX_CREDENTIAL_NS + "MembershipCredential", Map.of("id", DID, "holderIdentifier", BPN))),
                    Arguments.of(vc(CX_CREDENTIAL_NS + "MembershipCredential", Map.of("id", DID, CX_CREDENTIAL_NS + "holderIdentifier", BPN))));
        }
    }
    
    private static VerifiableCredential vc(String type, Map<String, Object> claims) {
        return VerifiableCredential.Builder.newInstance().type(type)
                .issuanceDate(Instant.now())
                .issuer(new Issuer("issuer", Map.of()))
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .id(claims.containsKey("id") ? claims.get("id").toString() : null)
                        .claims(claims)
                        .build())
                .build();
    }
    
    protected abstract MembershipCredentialIdExtractionFunction extractionFunction();
    
    protected abstract String expectedId();
}
