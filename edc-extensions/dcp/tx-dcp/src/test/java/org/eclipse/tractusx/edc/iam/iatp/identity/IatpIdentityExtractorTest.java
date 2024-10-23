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

package org.eclipse.tractusx.edc.iam.iatp.identity;

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
import static org.eclipse.edc.participant.spi.ParticipantAgent.PARTICIPANT_IDENTITY;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.CX_CREDENTIAL_NS;

class IatpIdentityExtractorTest {

    private static final String IDENTITY = "identity";
    private final IatpIdentityExtractor extractor = new IatpIdentityExtractor();

    private static VerifiableCredential vc(String type, Map<String, Object> claims) {
        return VerifiableCredential.Builder.newInstance().type(type)
                .issuanceDate(Instant.now())
                .issuer(new Issuer("issuer", Map.of()))
                .credentialSubject(CredentialSubject.Builder.newInstance().claims(claims).build())
                .build();
    }

    @ParameterizedTest
    @ArgumentsSource(VerifiableCredentialArgumentProvider.class)
    void attributesFor(VerifiableCredential credential) {
        var attributes = extractor.attributesFor(ClaimToken.Builder.newInstance().claim("vc", List.of(credential)).build());
        assertThat(attributes).containsEntry(PARTICIPANT_IDENTITY, IDENTITY);
    }

    @Test
    void attributesFor_fails_WhenCredentialNotFound() {
        assertThatThrownBy(() -> extractor.attributesFor(ClaimToken.Builder.newInstance().claim("vc", List.of(vc("FooCredential", Map.of("foo", "bar")))).build()))
                .isInstanceOf(EdcException.class)
                .hasMessage("Required credential type 'MembershipCredential' not present in ClaimToken, cannot extract property 'holderIdentifier'");
    }

    @Test
    void attributesFor_fails_whenNoVcClaims() {
        assertThatThrownBy(() -> extractor.attributesFor(ClaimToken.Builder.newInstance().build()))
                .isInstanceOf(EdcException.class)
                .hasMessageContaining("Failed to fetch credentials from the claim token: ClaimToken did not contain a 'vc' claim");
    }

    @Test
    void attributesFor_fails_whenNullVcClaims() {

        assertThatThrownBy(() -> extractor.attributesFor(ClaimToken.Builder.newInstance().claim("vc", null).build()))
                .isInstanceOf(EdcException.class)
                .hasMessageContaining("Failed to fetch credentials from the claim token: ClaimToken did not contain a 'vc' claim");
    }

    @Test
    void attributesFor_fails_WhenVcClaimIsNotList() {
        assertThatThrownBy(() -> extractor.attributesFor(ClaimToken.Builder.newInstance().claim("vc", "wrong").build()))
                .isInstanceOf(EdcException.class)
                .hasMessageContaining("Failed to fetch credentials from the claim token: ClaimToken contains a 'vc' claim, but the type is incorrect. Expected java.util.List, got java.lang.String.");
    }

    @Test
    void attributesFor_fails_WhenVcClaimsIsEmptyList() {
        assertThatThrownBy(() -> extractor.attributesFor(ClaimToken.Builder.newInstance().claim("vc", List.of()).build()))
                .isInstanceOf(EdcException.class)
                .hasMessageContaining("Failed to fetch credentials from the claim token: ClaimToken contains a 'vc' claim but it did not contain any VerifiableCredentials.");
    }

    private static class VerifiableCredentialArgumentProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of(vc("MembershipCredential", Map.of("holderIdentifier", IDENTITY))),
                    Arguments.of(vc(CX_CREDENTIAL_NS + "MembershipCredential", Map.of("holderIdentifier", IDENTITY))),
                    Arguments.of(vc(CX_CREDENTIAL_NS + "MembershipCredential", Map.of(CX_CREDENTIAL_NS + "holderIdentifier", IDENTITY))));
        }

    }
}
