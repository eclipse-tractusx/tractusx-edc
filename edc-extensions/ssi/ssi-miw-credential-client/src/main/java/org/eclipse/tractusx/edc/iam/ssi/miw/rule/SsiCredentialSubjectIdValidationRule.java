/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.iam.ssi.miw.rule;

import jakarta.json.JsonObject;
import org.eclipse.edc.jwt.spi.TokenValidationRule;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
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
