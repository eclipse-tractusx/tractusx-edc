/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
import org.eclipse.edc.iam.verifiablecredentials.rules.HasValidIssuer;
import org.eclipse.edc.iam.verifiablecredentials.rules.HasValidSubjectIds;
import org.eclipse.edc.iam.verifiablecredentials.rules.HasValidSubjectSchema;
import org.eclipse.edc.iam.verifiablecredentials.rules.IsInValidityPeriod;
import org.eclipse.edc.iam.verifiablecredentials.rules.IsNotRevoked;
import org.eclipse.edc.iam.verifiablecredentials.spi.VerifiableCredentialValidationService;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.RevocationServiceRegistry;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentationContainer;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.CredentialValidationRule;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.PresentationVerifier;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.TrustedIssuerRegistry;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

public class VerifiableCredentialValidationServiceImpl implements VerifiableCredentialValidationService {
    private final PresentationVerifier presentationVerifier;
    private final TrustedIssuerRegistry trustedIssuerRegistry;
    private final RevocationServiceRegistry revocationServiceRegistry;
    private final Clock clock;
    private final ObjectMapper mapper;

    public VerifiableCredentialValidationServiceImpl(PresentationVerifier presentationVerifier, TrustedIssuerRegistry trustedIssuerRegistry, RevocationServiceRegistry revocationServiceRegistry, Clock clock, ObjectMapper mapper) {
        this.presentationVerifier = presentationVerifier;
        this.trustedIssuerRegistry = trustedIssuerRegistry;
        this.revocationServiceRegistry = revocationServiceRegistry;
        this.clock = clock;
        this.mapper = mapper;
    }

    @Override
    public Result<Void> validate(List<VerifiablePresentationContainer> presentations, String audience, Collection<? extends CredentialValidationRule> additionalRules) {
        if (presentations.isEmpty()) {
            return failure("No presentations to validate");
        }
        return presentations.stream().map(verifiablePresentation -> {
            var credentials = verifiablePresentation.presentation().getCredentials();
            // verify, that the VP and all VPs are cryptographically OK
            var presentationIssuer = verifiablePresentation.presentation().getHolder();
            return presentationVerifier.verifyPresentation(verifiablePresentation, audience)
                    .compose(u -> validateVerifiableCredentials(credentials, presentationIssuer, additionalRules));
        }).reduce(success(), Result::merge);
    }

    @NotNull
    private Result<Void> validateVerifiableCredentials(List<VerifiableCredential> credentials, String presentationHolder, Collection<? extends CredentialValidationRule> additionalRules) {

        // issuer trust and subject binding are checked first: the other rules may fetch remote resources referenced by the credential
        // (e.g. status lists, schemas), which must not happen for credentials of untrusted issuers
        var trustRule = combine(List.of(
                new HasValidIssuer(trustedIssuerRegistry),
                new HasValidSubjectIds(presentationHolder)));

        var baseRules = new ArrayList<CredentialValidationRule>(List.of(
                new IsInValidityPeriod(clock),
                new IsNotRevoked(revocationServiceRegistry),
                new HasValidSubjectSchema(mapper)));

        baseRules.addAll(additionalRules);
        var baseRule = combine(baseRules);

        return credentials
                .stream()
                .map(c -> trustRule.apply(c).compose(v -> baseRule.apply(c)))
                .reduce(Result::merge)
                .orElseGet(Result::success);
    }

    private CredentialValidationRule combine(List<? extends CredentialValidationRule> rules) {
        return rules.stream().reduce(t -> success(), CredentialValidationRule::and, CredentialValidationRule::and);
    }

}
