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

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.json.JsonObject;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.SummaryContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.spi.agent.ParticipantAgent.PARTICIPANT_IDENTITY;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.CredentialsNamespaces.CX_SUMMARY_NS_V1;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.CredentialsNamespaces.VP_PROPERTY;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.JsonLdTextFixtures.createObjectMapper;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.JsonLdTextFixtures.expand;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.SummaryCredential.SIMPLE_VP;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.SummaryCredential.SUMMARY_VP;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.SummaryCredential.SUMMARY_VP_NO_HOLDER;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.SummaryCredential.SUMMARY_VP_NO_SUBJECT;

public class CredentialIdentityExtractorTest {

    static final Map<String, String> CONTEXT_CACHE = Map.of(CX_SUMMARY_NS_V1, SummaryContext.SUMMARY_CONTEXT);

    CredentialIdentityExtractor extractor = new CredentialIdentityExtractor();

    @Test
    void attributeFor() throws JsonProcessingException {
        var vp = expand(createObjectMapper().readValue(SUMMARY_VP, JsonObject.class), CONTEXT_CACHE);
        var attributes = extractor.attributesFor(ClaimToken.Builder.newInstance().claim(VP_PROPERTY, vp).build());

        assertThat(attributes).contains(Map.entry(PARTICIPANT_IDENTITY, "BPN of holder"));
    }

    @Test
    void attributeFor_exception_whenVpNotPresent() {
        assertThatThrownBy(() -> extractor.attributesFor(ClaimToken.Builder.newInstance().build()))
                .isInstanceOf(EdcException.class)
                .hasMessage("Failed to extract identity from the membership credential");
    }

    @Test
    void attributeFor_exception_whenCredentialTypeNotMatch() throws JsonProcessingException {
        var vp = expand(createObjectMapper().readValue(SIMPLE_VP, JsonObject.class), CONTEXT_CACHE);
        assertThatThrownBy(() -> extractor.attributesFor(ClaimToken.Builder.newInstance().claim(VP_PROPERTY, vp).build()))
                .isInstanceOf(EdcException.class)
                .hasMessage("Failed to extract identity from the membership credential");
    }

    @Test
    void attributeFor_exception_whenHolderIdentifierNotFound() throws JsonProcessingException {
        var vp = expand(createObjectMapper().readValue(SUMMARY_VP_NO_HOLDER, JsonObject.class), CONTEXT_CACHE);
        assertThatThrownBy(() -> extractor.attributesFor(ClaimToken.Builder.newInstance().claim(VP_PROPERTY, vp).build()))
                .isInstanceOf(EdcException.class)
                .hasMessage("Identity extractor: no holderIdentifier found");
    }

    @Test
    void attributeFor_exception_whenCredentialSubjectNotFound() throws JsonProcessingException {
        var vp = expand(createObjectMapper().readValue(SUMMARY_VP_NO_SUBJECT, JsonObject.class), CONTEXT_CACHE);
        assertThatThrownBy(() -> extractor.attributesFor(ClaimToken.Builder.newInstance().claim(VP_PROPERTY, vp).build()))
                .isInstanceOf(EdcException.class)
                .hasMessage("Identity extractor: no credentialSubject found");
    }
}
