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

package org.eclipse.tractusx.edc.iam.ssi.miw.credentials;

import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.tractusx.edc.iam.ssi.miw.api.MiwApiClient;
import org.eclipse.tractusx.edc.iam.ssi.spi.SsiCredentialClient;

import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.eclipse.tractusx.edc.iam.ssi.miw.api.MiwApiClient.VP;

public class SsiMiwCredentialClient implements SsiCredentialClient {

    private final MiwApiClient apiClient;

    public SsiMiwCredentialClient(MiwApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public Result<TokenRepresentation> obtainClientCredentials(TokenParameters parameters) {
        // TODO will need to take from the TokenParameters which are the credentials needed, REF https://github.com/eclipse-edc/Connector/pull/3150
        return apiClient.getCredentials(Set.of(), parameters.getAudience())
                .compose(credentials -> createPresentation(credentials, parameters))
                .compose(this::createToken);
    }
    
    @Override
    public Result<ClaimToken> validate(TokenRepresentation tokenRepresentation) {
        return extractClaims(tokenRepresentation)
                .compose(claimToken -> validatePresentation(claimToken, tokenRepresentation));
    }

    private Result<TokenRepresentation> createToken(Map<String, Object> presentationResponse) {
        var vp = presentationResponse.get(VP);
        if (vp instanceof String) {
            return Result.success(TokenRepresentation.Builder.newInstance().token((String) vp).build());
        } else {
            return Result.failure("Missing or invalid format for Verifiable Presentation");
        }
    }

    private Result<Map<String, Object>> createPresentation(List<Map<String, Object>> credentials, TokenParameters tokenParameters) {
        if (!credentials.isEmpty()) {
            return apiClient.createPresentation(credentials, tokenParameters.getAudience());
        } else {
            return Result.failure("Cannot create a presentation from an empty credentials list");
        }
    }

    private Result<ClaimToken> validatePresentation(ClaimToken claimToken, TokenRepresentation tokenRepresentation) {
        return apiClient.verifyPresentation(tokenRepresentation.getToken())
                .compose(v -> Result.success(claimToken));
    }

    private Result<ClaimToken> extractClaims(TokenRepresentation tokenRepresentation) {
        try {
            var jwt = SignedJWT.parse(tokenRepresentation.getToken());

            var tokenBuilder = ClaimToken.Builder.newInstance();
            jwt.getJWTClaimsSet().getClaims().entrySet().stream()
                    .filter(entry -> entry.getValue() != null)
                    .forEach(entry -> tokenBuilder.claim(entry.getKey(), entry.getValue()));

            return Result.success(tokenBuilder.build());
        } catch (ParseException e) {
            return Result.failure(e.getMessage());
        }
    }

}
