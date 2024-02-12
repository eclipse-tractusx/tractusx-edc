/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.iam.ssi.miw.rule;

import jakarta.json.JsonObject;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.JsonLdFieldExtractor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.CredentialsNamespaces.CREDENTIAL_ISSUER;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.CredentialsNamespaces.SUMMARY_CREDENTIAL_TYPE;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.CredentialsNamespaces.VP_PROPERTY;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.JsonLdTypeFunctions.extractObjectsOfType;

/**
 * {@link TokenValidationRule} that compares the issuer in the Verifiable Credential (Summary) with the one provided
 * by configuration.
 */
public class SsiCredentialIssuerValidationRule implements TokenValidationRule {

    private static final String SUBJECT_ISSUER_EXTRACTOR_PREFIX = "Credential issuer extractor:";

    private static final String SUBJECT_ISSUER_FIELD_ALIAS = "issuer";

    private final String credentialIssuer;

    private final Monitor monitor;

    private final JsonLdFieldExtractor credentialIssuerExtractor = JsonLdFieldExtractor.Builder.newInstance()
            .field(CREDENTIAL_ISSUER)
            .fieldAlias(SUBJECT_ISSUER_FIELD_ALIAS)
            .errorPrefix(SUBJECT_ISSUER_EXTRACTOR_PREFIX)
            .build();

    public SsiCredentialIssuerValidationRule(String credentialIssuer, Monitor monitor) {
        this.credentialIssuer = credentialIssuer;
        this.monitor = monitor;
    }

    @Override
    public Result<Void> checkRule(@NotNull ClaimToken toVerify, @Nullable Map<String, Object> additional) {

        var vp = (JsonObject) toVerify.getClaim(VP_PROPERTY);

        return Optional.ofNullable(vp)
                .map(v -> extractObjectsOfType(SUMMARY_CREDENTIAL_TYPE, v))
                .orElse(Stream.empty())
                .map(this::extractIssuer)
                .findFirst()
                .orElseGet(() -> Result.failure("Failed to extract credential subject from the membership credential"))
                .compose(this::validateCredentialIssuer)
                .onFailure(failure -> monitor.severe(failure.getFailureDetail()));

    }

    private Result<Void> validateCredentialIssuer(String extractedCredentialIssuer) {
        if (credentialIssuer.equals(extractedCredentialIssuer)) {
            return Result.success();
        } else {
            return Result.failure(format("Invalid credential issuer: expected %s, found %s", credentialIssuer, extractedCredentialIssuer));
        }
    }

    private Result<String> extractIssuer(JsonObject credential) {
        return this.credentialIssuerExtractor.extract(credential)
                .compose(this::extractIssuerValue);
    }

    private Result<String> extractIssuerValue(JsonObject issuer) {
        var issuerValue = issuer.getString(ID);
        if (issuerValue == null) {
            return Result.failure("Failed to find the issuer");
        } else {
            return Result.success(issuerValue);
        }
    }
}
