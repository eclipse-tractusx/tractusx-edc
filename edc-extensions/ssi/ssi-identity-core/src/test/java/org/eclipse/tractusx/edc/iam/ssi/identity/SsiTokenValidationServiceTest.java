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

package org.eclipse.tractusx.edc.iam.ssi.identity;

import org.eclipse.edc.jwt.spi.TokenValidationRule;
import org.eclipse.edc.jwt.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.tractusx.edc.iam.ssi.spi.SsiCredentialClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class SsiTokenValidationServiceTest {

    SsiCredentialClient credentialClient = mock(SsiCredentialClient.class);
    TokenValidationRulesRegistry validationRulesRegistry = mock(TokenValidationRulesRegistry.class);

    SsiTokenValidationService validationService;

    @BeforeEach
    void setup() {
        validationService = new SsiTokenValidationService(validationRulesRegistry, credentialClient);
    }
    
    @Test
    void validate_success() {
        var token = TokenRepresentation.Builder.newInstance().token("test").build();
        var rule = mock(TokenValidationRule.class);
        var claim = ClaimToken.Builder.newInstance().build();

        when(validationRulesRegistry.getRules()).thenReturn(List.of(rule));
        when(credentialClient.validate(token)).thenReturn(Result.success(claim));
        when(rule.checkRule(any(), any())).thenReturn(Result.success());

        var result = validationService.validate(token);

        assertThat(result).isNotNull().extracting(Result::getContent).isEqualTo(claim);

        verify(credentialClient).validate(token);
        verify(rule).checkRule(eq(claim), any());
    }

    @Test
    void validate_fail_whenClientFails() {
        var token = TokenRepresentation.Builder.newInstance().token("test").build();
        var rule = mock(TokenValidationRule.class);

        when(validationRulesRegistry.getRules()).thenReturn(List.of(rule));
        when(credentialClient.validate(token)).thenReturn(Result.failure("failure"));
        when(rule.checkRule(any(), any())).thenReturn(Result.success());

        var result = validationService.validate(token);

        assertThat(result).isNotNull().matches(Result::failed);

        verify(credentialClient).validate(token);
        verifyNoInteractions(rule);
    }

    @Test
    void validate_fail_whenRuleFails() {
        var token = TokenRepresentation.Builder.newInstance().token("test").build();
        var rule = mock(TokenValidationRule.class);
        var claim = ClaimToken.Builder.newInstance().build();


        when(validationRulesRegistry.getRules()).thenReturn(List.of(rule));
        when(credentialClient.validate(token)).thenReturn(Result.success(claim));
        when(rule.checkRule(any(), any())).thenReturn(Result.failure("failure"));

        var result = validationService.validate(token);

        assertThat(result).isNotNull().matches(Result::failed);

        verify(credentialClient).validate(token);
        verify(rule).checkRule(eq(claim), any());
    }
}
