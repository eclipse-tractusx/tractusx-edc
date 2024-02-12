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
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUER;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.CredentialsNamespaces.CREDENTIAL_SUBJECT;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.CredentialsNamespaces.SUMMARY_CREDENTIAL_TYPE;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.CredentialsNamespaces.VP_PROPERTY;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.JsonLdTypeFunctions.extractObjectsOfType;

/**
 * {@link TokenValidationRule} that compares the issuer of the VP (JWT format) with the credential subject id of
 * the Verifiable Credential (Summary)
 */
public class SsiCredentialSubjectIdValidationRule implements TokenValidationRule {

    private static final String CREDENTIAL_SUBJECT_EXTRACTOR_PREFIX = "Credential subject extractor:";
    private static final String CREDENTIAL_SUBJECT_FIELD_ALIAS = "credentialSubject";

    private final Monitor monitor;

    private final JsonLdFieldExtractor credentialSubjectExtractor = JsonLdFieldExtractor.Builder.newInstance()
            .field(CREDENTIAL_SUBJECT)
            .fieldAlias(CREDENTIAL_SUBJECT_FIELD_ALIAS)
            .errorPrefix(CREDENTIAL_SUBJECT_EXTRACTOR_PREFIX)
            .build();

    public SsiCredentialSubjectIdValidationRule(Monitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public Result<Void> checkRule(@NotNull ClaimToken toVerify, @Nullable Map<String, Object> additional) {
        var issuer = toVerify.getStringClaim(ISSUER);

        if (issuer == null) {
            return Result.failure("Required issuer (iss) claim is missing in token");
        }
        var vp = (JsonObject) toVerify.getClaim(VP_PROPERTY);

        return Optional.ofNullable(vp)
                .map(v -> extractObjectsOfType(SUMMARY_CREDENTIAL_TYPE, v))
                .orElse(Stream.empty())
                .map(this::extractSubjectId)
                .findFirst()
                .orElseGet(() -> Result.failure("Failed to extract credential subject from the membership credential"))
                .compose(credentialSubjectId -> validateCredentialSubjectId(credentialSubjectId, issuer))
                .onFailure((failure -> monitor.severe(failure.getFailureDetail())));

    }

    private Result<Void> validateCredentialSubjectId(String credentialSubjectId, String issuer) {
        if (issuer.equals(credentialSubjectId)) {
            return Result.success();
        } else {
            return Result.failure(format("Issuer %s and credential subject id %s don't match", issuer, credentialSubjectId));
        }
    }

    private Result<String> extractSubjectId(JsonObject credential) {
        return this.credentialSubjectExtractor.extract(credential)
                .compose(this::extractId);
    }

    private Result<String> extractId(JsonObject credentialSubject) {
        var id = credentialSubject.getString(ID);
        if (id == null) {
            return Result.failure("Failed to find the id in credential subject");
        } else {
            return Result.success(id);
        }
    }
}
