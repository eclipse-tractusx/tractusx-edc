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

package org.eclipse.tractusx.edc.iam.ssi.identity.rule;

import org.eclipse.edc.jwt.spi.TokenValidationRule;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;

public class SsiAudienceValidationRuleTest {

    private final String endpointAudience = "test-audience";
    private final TokenValidationRule rule = new SsiAudienceValidationRule(endpointAudience);

    @Test
    void validAudience() {
        var token = ClaimToken.Builder.newInstance()
                .claim(AUDIENCE, List.of(endpointAudience))
                .build();

        var result = rule.checkRule(token, emptyMap());

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void validationKoBecauseAudienceNotRespected() {
        var token = ClaimToken.Builder.newInstance()
                .claim(AUDIENCE, List.of("fake-audience"))
                .build();

        var result = rule.checkRule(token, emptyMap());

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).hasSize(1)
                .contains("Token audience (aud) claim did not contain audience: test-audience");
    }

    @Test
    void validationKoBecauseAudienceNotProvided() {
        var token = ClaimToken.Builder.newInstance()
                .build();

        var result = rule.checkRule(token, emptyMap());

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).hasSize(1)
                .contains("Required audience (aud) claim is missing in token");
    }
}
