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
import jakarta.json.Json;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.tractusx.edc.iam.ssi.miw.api.MiwApiClient;
import org.eclipse.tractusx.edc.iam.ssi.spi.SsiCredentialClient;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.eclipse.tractusx.edc.iam.ssi.miw.api.MiwApiClient.VP;

public class SsiMiwCredentialClient implements SsiCredentialClient {

    private final MiwApiClient apiClient;

    private final JsonLd jsonLdService;
    private final Monitor monitor;

    public SsiMiwCredentialClient(MiwApiClient apiClient, JsonLd jsonLdService, Monitor monitor) {
        this.apiClient = apiClient;
        this.jsonLdService = jsonLdService;
        this.monitor = monitor;
    }

    @Override
    public Result<TokenRepresentation> obtainClientCredentials(TokenParameters parameters) {
        return apiClient.getCredentials(parameters.getAdditional().keySet())
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
        return claimToken.getListClaim(AUDIENCE).stream().map(String.class::cast).findFirst()
                .map(audience -> apiClient.verifyPresentation(tokenRepresentation.getToken(), audience)
                        .compose(v -> Result.success(claimToken)))
                .orElseGet(() -> Result.failure("Required audience (aud) claim is missing in token"));
    }

    private Result<ClaimToken> extractClaims(TokenRepresentation tokenRepresentation) {
        try {
            var jwt = SignedJWT.parse(tokenRepresentation.getToken());

            var tokenBuilder = ClaimToken.Builder.newInstance();
            jwt.getJWTClaimsSet().getClaims().entrySet().stream()
                    .filter(entry -> entry.getValue() != null)
                    .map(this::mapClaim)
                    .peek(this::logIfError)
                    .filter(Result::succeeded)
                    .map(Result::getContent)
                    .forEach(entry -> tokenBuilder.claim(entry.getKey(), entry.getValue()));

            return Result.success(tokenBuilder.build());
        } catch (ParseException e) {
            return Result.failure(e.getMessage());
        }
    }

    private Result<Map.Entry<String, Object>> mapClaim(Map.Entry<String, Object> entry) {
        if (entry.getKey().equals(VP)) {
            var json = Json.createObjectBuilder((Map<String, Object>) entry.getValue()).build();
            return jsonLdService.expand(json)
                    .map((expanded) -> Map.entry(entry.getKey(), expanded));
        } else {
            return Result.success(entry);
        }
    }

    private void logIfError(Result<?> result) {
        result.onFailure(f -> monitor.warning(f.getFailureDetail()));
    }
}
