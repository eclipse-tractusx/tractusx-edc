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

package org.eclipse.tractusx.edc.iam.ssi.identity.extractor;

import jakarta.json.JsonObject;
import org.eclipse.edc.spi.agent.ParticipantAgentServiceExtension;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.JsonLdFieldExtractor;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static org.eclipse.edc.spi.agent.ParticipantAgent.PARTICIPANT_IDENTITY;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.CredentialsNamespaces.CREDENTIAL_SUBJECT;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.CredentialsNamespaces.HOLDER_IDENTIFIER;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.CredentialsNamespaces.SUMMARY_CREDENTIAL_TYPE;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.CredentialsNamespaces.VP_PROPERTY;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.JsonLdTypeFunctions.extractObjectsOfType;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.JsonLdValueFunctions.extractStringValue;

public class SummaryCredentialIdentityExtractor implements ParticipantAgentServiceExtension {


    private final JsonLdFieldExtractor holderIdentifierExtractor = JsonLdFieldExtractor.Builder.newInstance()
            .field(HOLDER_IDENTIFIER)
            .fieldAlias("holderIdentifier")
            .build();
    private final JsonLdFieldExtractor credentialSubjectExtractor = JsonLdFieldExtractor.Builder.newInstance()
            .field(CREDENTIAL_SUBJECT)
            .fieldAlias("credentialSubject")
            .build();

    @Override
    public @NotNull Map<String, String> attributesFor(ClaimToken token) {
        var vp = (JsonObject) token.getClaim(VP_PROPERTY);

        if (vp != null) {
            var attributes = new HashMap<String, String>();
            extractObjectsOfType(SUMMARY_CREDENTIAL_TYPE, vp)
                    .map(this::extractHolderIdentifier)
                    .filter(Result::succeeded)
                    .map(Result::getContent)
                    .findFirst()
                    .ifPresent((bpn) -> {
                        attributes.put(PARTICIPANT_IDENTITY, bpn);
                    });
            return attributes;
        } else {
            return Map.of();
        }
    }

    private Result<String> extractHolderIdentifier(JsonObject credential) {
        return this.credentialSubjectExtractor.extract(credential)
                .compose(holderIdentifierExtractor::extract)
                .compose(this::extractHolderIdentifierValue);
    }

    private Result<String> extractHolderIdentifierValue(JsonObject identifier) {
        var bpn = extractStringValue(identifier);
        if (bpn == null) {
            return Result.failure("Failed to find the holder identifier");
        } else {
            return Result.success(bpn);
        }
    }
}
