/********************************************************************************
 * Copyright (c) 2026 Cofinity-X GmbH
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

package org.eclipse.edc.iam.verifiablecredentials;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.RevocationServiceRegistry;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentation;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentationContainer;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.CredentialValidationRule;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.PresentationVerifier;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.TrustedIssuerRegistry;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class VerifiableCredentialValidationServiceImplTest {

    private static final String HOLDER = "did:web:holder";
    private static final String ISSUER = "did:web:issuer";
    private static final String AUDIENCE = "did:web:verifier";

    private final PresentationVerifier presentationVerifier = mock();
    private final TrustedIssuerRegistry trustedIssuerRegistry = mock();
    private final RevocationServiceRegistry revocationServiceRegistry = mock();
    private final VerifiableCredentialValidationServiceImpl service = new VerifiableCredentialValidationServiceImpl(presentationVerifier,
            trustedIssuerRegistry, revocationServiceRegistry, Clock.systemUTC(), new ObjectMapper());

    @BeforeEach
    void setUp() {
        when(presentationVerifier.verifyPresentation(any(), anyString())).thenReturn(Result.success());
        when(revocationServiceRegistry.checkValidity(any())).thenReturn(Result.success());
    }

    @Test
    void validate_trustedIssuer_succeeds() {
        when(trustedIssuerRegistry.getSupportedTypes(any())).thenReturn(Set.of(TrustedIssuerRegistry.WILDCARD));

        var result = service.validate(List.of(presentation(credential(ISSUER, HOLDER))), AUDIENCE, List.of());

        assertThat(result).isSucceeded();
        verify(revocationServiceRegistry).checkValidity(any());
    }

    @Test
    void validate_untrustedIssuer_doesNotConsultRemoteRules() {
        when(trustedIssuerRegistry.getSupportedTypes(any())).thenReturn(Set.of());
        CredentialValidationRule additionalRule = mock();

        var result = service.validate(List.of(presentation(credential("did:web:unknown", HOLDER))), AUDIENCE, List.of(additionalRule));

        assertThat(result).isFailed().detail().contains("not supported for issuer 'did:web:unknown'");
        verifyNoInteractions(revocationServiceRegistry);
        verifyNoInteractions(additionalRule);
    }

    @Test
    void validate_subjectNotHolder_doesNotConsultRemoteRules() {
        when(trustedIssuerRegistry.getSupportedTypes(any())).thenReturn(Set.of(TrustedIssuerRegistry.WILDCARD));

        var result = service.validate(List.of(presentation(credential(ISSUER, "did:web:someone-else"))), AUDIENCE, List.of());

        assertThat(result).isFailed();
        verifyNoInteractions(revocationServiceRegistry);
    }

    @Test
    void validate_revoked_fails() {
        when(trustedIssuerRegistry.getSupportedTypes(any())).thenReturn(Set.of(TrustedIssuerRegistry.WILDCARD));
        when(revocationServiceRegistry.checkValidity(any())).thenReturn(Result.failure("revoked"));

        var result = service.validate(List.of(presentation(credential(ISSUER, HOLDER))), AUDIENCE, List.of());

        assertThat(result).isFailed().detail().contains("revoked");
    }

    @Test
    void validate_presentationVerificationFails_doesNotValidateCredentials() {
        when(presentationVerifier.verifyPresentation(any(), anyString())).thenReturn(Result.failure("invalid signature"));

        var result = service.validate(List.of(presentation(credential(ISSUER, HOLDER))), AUDIENCE, List.of());

        assertThat(result).isFailed().detail().contains("invalid signature");
        verifyNoInteractions(trustedIssuerRegistry);
        verify(revocationServiceRegistry, never()).checkValidity(any());
    }

    @Test
    void validate_noPresentations_fails() {
        assertThat(service.validate(List.of(), AUDIENCE, List.of())).isFailed();
    }

    private VerifiablePresentationContainer presentation(VerifiableCredential credential) {
        var vp = VerifiablePresentation.Builder.newInstance()
                .type("VerifiablePresentation")
                .holder(HOLDER)
                .credential(credential)
                .build();
        return new VerifiablePresentationContainer("raw", CredentialFormat.VC1_0_JWT, vp);
    }

    private VerifiableCredential credential(String issuer, String subjectId) {
        return VerifiableCredential.Builder.newInstance()
                .id("urn:uuid:credential")
                .type("MembershipCredential")
                .issuer(new Issuer(issuer))
                .issuanceDate(Instant.now().minusSeconds(60))
                .credentialSubject(CredentialSubject.Builder.newInstance().id(subjectId).claim("holderIdentifier", "BPNL000000000001").build())
                .build();
    }
}
