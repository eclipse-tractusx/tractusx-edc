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

import org.eclipse.edc.jwt.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.jwt.spi.TokenValidationService;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.tractusx.edc.iam.ssi.spi.SsiCredentialClient;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class SsiTokenValidationService implements TokenValidationService {

    private final TokenValidationRulesRegistry rulesRegistry;
    private final SsiCredentialClient credentialClient;

    public SsiTokenValidationService(TokenValidationRulesRegistry rulesRegistry, SsiCredentialClient credentialClient) {
        this.rulesRegistry = rulesRegistry;
        this.credentialClient = credentialClient;
    }

    @Override
    public Result<ClaimToken> validate(TokenRepresentation tokenRepresentation) {
        return credentialClient.validate(tokenRepresentation)
                .compose(claimToken -> checkRules(claimToken, tokenRepresentation.getAdditional()));
    }

    private Result<ClaimToken> checkRules(ClaimToken claimToken, @Nullable Map<String, Object> additional) {
        var errors = rulesRegistry.getRules().stream()
                .map(r -> r.checkRule(claimToken, additional))
                .filter(Result::failed)
                .map(Result::getFailureMessages)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        if (!errors.isEmpty()) {
            return Result.failure(errors);
        }
        return Result.success(claimToken);
    }
}
