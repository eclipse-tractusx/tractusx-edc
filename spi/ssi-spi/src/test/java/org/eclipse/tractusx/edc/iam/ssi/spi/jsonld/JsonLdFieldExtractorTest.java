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

package org.eclipse.tractusx.edc.iam.ssi.spi.jsonld;

import jakarta.json.JsonObject;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.CredentialsNamespaces.CREDENTIAL_SUBJECT;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.CredentialsNamespaces.CX_SUMMARY_NS_V1;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.CredentialsNamespaces.HOLDER_IDENTIFIER;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.CredentialsNamespaces.SUMMARY_CREDENTIAL_TYPE;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.JsonLdTextFixtures.createObjectMapper;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.JsonLdTextFixtures.expand;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.JsonLdTypeFunctions.extractObjectsOfType;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.SummaryCredential.SUMMARY_VP;

public class JsonLdFieldExtractorTest {

    private static final Map<String, String> CONTEXT_CACHE = Map.of(CX_SUMMARY_NS_V1, SummaryContext.SUMMARY_CONTEXT);

    @Test
    void extract() throws Exception {
        var vp = expand(createObjectMapper().readValue(SUMMARY_VP, JsonObject.class), CONTEXT_CACHE);

        var extractor = JsonLdFieldExtractor.Builder.newInstance()
                .field(CREDENTIAL_SUBJECT)
                .fieldAlias("credentialSubject")
                .errorPrefix("prefix")
                .build();


        var summaryCredential = extractObjectsOfType(SUMMARY_CREDENTIAL_TYPE, vp).findFirst().orElseThrow();

        var subject = extractor.extract(summaryCredential);
        assertThat(subject).matches(Result::succeeded).extracting(Result::getContent)
                .satisfies(jsonObject -> assertThat(jsonObject.containsKey(HOLDER_IDENTIFIER)).isTrue());
        
    }

    @Test
    void extract_fail() throws Exception {
        var vp = expand(createObjectMapper().readValue(SUMMARY_VP, JsonObject.class), CONTEXT_CACHE);

        var extractor = JsonLdFieldExtractor.Builder.newInstance()
                .field(HOLDER_IDENTIFIER)
                .fieldAlias("holderIdentifier")
                .errorPrefix("prefix")
                .build();

        var summaryCredential = extractObjectsOfType(SUMMARY_CREDENTIAL_TYPE, vp).findFirst().orElseThrow();

        var subject = extractor.extract(summaryCredential);
        assertThat(subject).matches(Result::failed).extracting(Result::getFailureDetail)
                .satisfies(errorMessage -> {
                    assertThat(errorMessage).isEqualTo("prefix no holderIdentifier found");
                });

    }
}
