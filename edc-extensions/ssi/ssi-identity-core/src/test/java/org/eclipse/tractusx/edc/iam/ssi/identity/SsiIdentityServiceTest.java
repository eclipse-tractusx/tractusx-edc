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

import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.PublicKeyResolver;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.tractusx.edc.iam.ssi.spi.SsiCredentialClient;
import org.eclipse.tractusx.edc.iam.ssi.spi.SsiTokenValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SsiIdentityServiceTest {

    SsiCredentialClient credentialClient = mock(SsiCredentialClient.class);
    SsiTokenValidationService tokenValidationService = mock(SsiTokenValidationService.class);
    TokenValidationRulesRegistry rulesRegistry = mock(TokenValidationRulesRegistry.class);
    PublicKeyResolver publicKeyResolver = mock(PublicKeyResolver.class);

    SsiIdentityService identityService;

    @BeforeEach
    void setup() {
        identityService = new SsiIdentityService(tokenValidationService, rulesRegistry, credentialClient);
    }

    @Test
    void verifyJwtToken_success() {
        var token = TokenRepresentation.Builder.newInstance().token("test").build();
        var claim = ClaimToken.Builder.newInstance().build();

        when(tokenValidationService.validate(eq(token), eq(List.of()))).thenReturn(Result.success(claim));

        var result = identityService.verifyJwtToken(token, "audience");

        assertThat(result).isNotNull().extracting(Result::getContent).isEqualTo(claim);
    }

    @Test
    void verifyJwtToken_failed() {
        var token = TokenRepresentation.Builder.newInstance().token("test").build();

        when(tokenValidationService.validate(eq(token), eq(List.of()))).thenReturn(Result.failure("fail"));

        var result = identityService.verifyJwtToken(token, "audience");

        assertThat(result).isNotNull().matches(Result::failed);
    }


    @Test
    void obtainClientCredentials_success() {
        var tokenParameters = TokenParameters.Builder.newInstance().claims(AUDIENCE, "audience").build();
        var tokenRepresentation = TokenRepresentation.Builder.newInstance().token("test").build();

        when(credentialClient.obtainClientCredentials(tokenParameters)).thenReturn(Result.success(tokenRepresentation));

        var result = identityService.obtainClientCredentials(tokenParameters);

        assertThat(result).isNotNull().extracting(Result::getContent).isEqualTo(tokenRepresentation);
    }

    @Test
    void obtainClientCredentials_fail() {
        var tokenParameters = TokenParameters.Builder.newInstance().claims(AUDIENCE, "audience").build();

        when(credentialClient.obtainClientCredentials(tokenParameters)).thenReturn(Result.failure("fail"));

        var result = identityService.obtainClientCredentials(tokenParameters);

        assertThat(result).isNotNull().matches(Result::failed);
    }
}
