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

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.json.JsonObject;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.SummaryContext;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.CredentialsNamespaces.CX_SUMMARY_NS_V1;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.CredentialsNamespaces.VP_PROPERTY;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.JsonLdTextFixtures.createObjectMapper;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.JsonLdTextFixtures.expand;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.SummaryCredential.SUMMARY_VP;
import static org.mockito.Mockito.mock;

public class SsiCredentialIssuerValidationRuleTest {

    static final Map<String, String> CONTEXT_CACHE = Map.of(CX_SUMMARY_NS_V1, SummaryContext.SUMMARY_CONTEXT);

    SsiCredentialIssuerValidationRule validationRule;

    @Test
    void checkRule() throws JsonProcessingException {
        validationRule = new SsiCredentialIssuerValidationRule(Set.of("did:web:issuer-a016-203-129-213-99.ngrok-free.app:BPNL000000000000"), mock(Monitor.class));
        var vp = expand(createObjectMapper().readValue(SUMMARY_VP, JsonObject.class), CONTEXT_CACHE);
        var claimToken = ClaimToken.Builder.newInstance().claim(VP_PROPERTY, vp).build();
        var result = validationRule.checkRule(claimToken, Map.of());

        assertThat(result.succeeded()).isTrue();
    }


    @Test
    void checkRule_shouldFail_whenIssuerIsWrong() throws JsonProcessingException {
        validationRule = new SsiCredentialIssuerValidationRule(Set.of("issuer"), mock(Monitor.class));
        var vp = expand(createObjectMapper().readValue(SUMMARY_VP, JsonObject.class), CONTEXT_CACHE);
        var claimToken = ClaimToken.Builder.newInstance().claim(VP_PROPERTY, vp).build();
        var result = validationRule.checkRule(claimToken, Map.of());

        assertThat(result.succeeded()).isFalse();
    }
}
