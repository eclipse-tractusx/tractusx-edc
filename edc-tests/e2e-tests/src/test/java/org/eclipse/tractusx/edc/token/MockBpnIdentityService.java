/*
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.token;

import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static java.lang.String.format;

/**
 * An {@link IdentityService} that will inject the BPN claim in every token.
 * Please only use in testing scenarios!
 */
public class MockBpnIdentityService implements IdentityService {

    private static final String BUSINESS_PARTNER_NUMBER_CLAIM = "BusinessPartnerNumber";
    private final String businessPartnerNumber;
    private final Monitor monitor;
    private final TypeManager typeManager = new TypeManager();

    public MockBpnIdentityService(String businessPartnerNumber, @NotNull Monitor monitor) {
        this.businessPartnerNumber = businessPartnerNumber;
        this.monitor = monitor;
    }

    @Override
    public Result<TokenRepresentation> obtainClientCredentials(TokenParameters parameters) {
        var token = Map.of(BUSINESS_PARTNER_NUMBER_CLAIM, businessPartnerNumber);

        var tokenRepresentation = TokenRepresentation.Builder.newInstance()
                .token(typeManager.writeValueAsString(token))
                .build();
        monitor.debug("BPNIDENTITYSERVICE: OBTAIN CLIENT CREDENTIALS");
        return Result.success(tokenRepresentation);
    }

    @Override
    public Result<ClaimToken> verifyJwtToken(TokenRepresentation tokenRepresentation, String audience) {

        var token = typeManager.readValue(tokenRepresentation.getToken(), Map.class);
        monitor.debug("BPNIDENTITYSERVICE: VERIFY JTW TOKEN");
        if (token.containsKey(BUSINESS_PARTNER_NUMBER_CLAIM)) {
            return Result.success(ClaimToken.Builder.newInstance()
                    .claim(BUSINESS_PARTNER_NUMBER_CLAIM, token.get(BUSINESS_PARTNER_NUMBER_CLAIM))
                    .build());
        }
        return Result.failure(format("Expected %s claim, but token did not contain them", BUSINESS_PARTNER_NUMBER_CLAIM));
    }

}
