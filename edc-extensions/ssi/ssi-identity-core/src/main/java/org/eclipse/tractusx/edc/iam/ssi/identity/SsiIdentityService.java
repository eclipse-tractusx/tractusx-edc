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
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.iam.VerificationContext;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.tractusx.edc.iam.ssi.spi.SsiCredentialClient;
import org.eclipse.tractusx.edc.iam.ssi.spi.SsiTokenValidationService;

import static org.eclipse.tractusx.edc.iam.ssi.spi.SsiConstants.SSI_TOKEN_CONTEXT;

public class SsiIdentityService implements IdentityService {

    private final SsiTokenValidationService tokenValidationService;

    private final TokenValidationRulesRegistry rulesRegistry;

    private final SsiCredentialClient client;

    public SsiIdentityService(SsiTokenValidationService tokenValidationService, TokenValidationRulesRegistry rulesRegistry,
                              SsiCredentialClient client) {
        this.tokenValidationService = tokenValidationService;
        this.rulesRegistry = rulesRegistry;
        this.client = client;
    }

    @Override
    public Result<TokenRepresentation> obtainClientCredentials(TokenParameters parameters) {
        return client.obtainClientCredentials(parameters);
    }

    @Override
    public Result<ClaimToken> verifyJwtToken(TokenRepresentation tokenRepresentation, VerificationContext verificationContext) {
        return tokenValidationService.validate(tokenRepresentation, rulesRegistry.getRules(SSI_TOKEN_CONTEXT));
    }
}
