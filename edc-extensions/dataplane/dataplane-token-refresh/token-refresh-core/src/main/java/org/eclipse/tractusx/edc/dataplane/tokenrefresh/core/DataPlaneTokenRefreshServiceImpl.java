/*
 *
 *   Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft
 *
 *   See the NOTICE file(s) distributed with this work for additional
 *   information regarding copyright ownership.
 *
 *   This program and the accompanying materials are made available under the
 *   terms of the Apache License, Version 2.0 which is available at
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *   WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *   License for the specific language governing permissions and limitations
 *   under the License.
 *
 *   SPDX-License-Identifier: Apache-2.0
 *
 */

package org.eclipse.tractusx.edc.dataplane.tokenrefresh.core;

import org.eclipse.edc.connector.dataplane.spi.AccessTokenData;
import org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAccessTokenService;
import org.eclipse.edc.connector.dataplane.spi.store.AccessTokenDataStore;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.token.spi.TokenDecorator;
import org.eclipse.edc.token.spi.TokenGenerationService;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.eclipse.tractusx.edc.dataplane.tokenrefresh.core.rules.ClaimIsPresentRule;
import org.eclipse.tractusx.edc.dataplane.tokenrefresh.core.rules.IssuerEqualsSubjectRule;
import org.eclipse.tractusx.edc.dataplane.tokenrefresh.core.rules.RefreshTokenMustExistRule;
import org.eclipse.tractusx.edc.dataplane.tokenrefresh.spi.DataPlaneTokenRefreshService;
import org.eclipse.tractusx.edc.dataplane.tokenrefresh.spi.model.TokenResponse;

import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * This implementation of the {@link DataPlaneTokenRefreshService} validates an incoming authentication token.
 */
public class DataPlaneTokenRefreshServiceImpl implements DataPlaneTokenRefreshService, DataPlaneAccessTokenService {
    public static final String ACCESS_TOKEN_CLAIM = "access_token";
    public static final String TOKEN_ID_CLAIM = "jti";
    public static final String REFRESH_TOKEN_PROPERTY = "refreshToken";
    private static final Long DEFAULT_EXPIRY_IN_SECONDS = 60 * 5L;
    private final List<TokenValidationRule> tokenValidationRules = List.of(new IssuerEqualsSubjectRule(), new ClaimIsPresentRule(ACCESS_TOKEN_CLAIM), new ClaimIsPresentRule(TOKEN_ID_CLAIM));
    private final TokenValidationService tokenValidationService;
    private final DidPublicKeyResolver publicKeyResolver;
    private final AccessTokenDataStore accessTokenDataStore;
    private final TokenGenerationService tokenGenerationService;
    private final Supplier<PrivateKey> privateKeySupplier;
    private final Monitor monitor;
    private final String refreshEndpoint;


    public DataPlaneTokenRefreshServiceImpl(TokenValidationService tokenValidationService,
                                            DidPublicKeyResolver publicKeyResolver,
                                            AccessTokenDataStore accessTokenDataStore,
                                            TokenGenerationService tokenGenerationService, Supplier<PrivateKey> privateKeySupplier, Monitor monitor, String refreshEndpoint) {
        this.tokenValidationService = tokenValidationService;
        this.publicKeyResolver = publicKeyResolver;
        this.accessTokenDataStore = accessTokenDataStore;
        this.tokenGenerationService = tokenGenerationService;
        this.privateKeySupplier = privateKeySupplier;
        this.monitor = monitor;
        this.refreshEndpoint = refreshEndpoint;
    }

    /**
     * Refreshes an incoming refresh token and authentication token. During validation of those tokens, the following steps are performed:
     *
     * <ul>
     *     <li>parse the {@code authenticationToken} into a {@link com.nimbusds.jwt.JWT}</li>
     *     <li>resolve the DID based on the {@code iss} claim</li>
     *     <li>resolve the public key material in the DID Document identified by the {@code kid} header</li>
     *     <li>verify the token's signature</li>
     *     <li>assert {@code iss} and {@code sub} claims are identical</li>
     *     <li>assert the the token contains an {@code access_token} claim, and that the value is identical to the access token we have on record</li>
     *     <li>assert that the {@code refreshToken} parameter is identical to the refresh token we have on record</li>
     * </ul>
     *
     * @param refreshToken        The refresh token that was issued in the original/previous token request.
     * @param authenticationToken A <a href="https://github.com/eclipse-tractusx/tractusx-profiles/blob/main/tx/refresh/refresh.token.grant.profile.md#31-client-authentication">client authentication token</a>
     */
    @Override
    public Result<TokenResponse> refreshToken(String refreshToken, String authenticationToken) {

        var allRules = new ArrayList<>(tokenValidationRules);
        allRules.add(new RefreshTokenMustExistRule(accessTokenDataStore, refreshToken));

        var accessTokenDataResult = resolveToken(authenticationToken, allRules);
        if (accessTokenDataResult.failed()) {
            return accessTokenDataResult.mapTo();
        }

        var existingAccessTokenData = accessTokenDataResult.getContent();

        var newTokenParams = TokenParameters.Builder.newInstance()
                .claims(existingAccessTokenData.claimToken().getClaims())
                .build();

        var newAccessToken = createToken(newTokenParams).map(tr -> tr.tokenRepresentation().getToken());
        var newRefreshToken = createToken(TokenParameters.Builder.newInstance().build()).map(tr -> tr.tokenRepresentation().getToken());
        if (newAccessToken.failed() || newRefreshToken.failed()) {
            var errors = new ArrayList<>(newAccessToken.getFailureMessages());
            errors.addAll(newRefreshToken.getFailureMessages());
            return Result.failure("Failed to regenerate access/refresh token pair: %s".formatted(errors));
        }

        // additional token information will be added to the TokenRepresentation, which will be returned to the caller
        // note: can't use DBI (double-bracket initialization) here, because SonarCloud will complain about it
        var accessTokenAdditional = new HashMap<>(existingAccessTokenData.additionalProperties());
        accessTokenAdditional.put(REFRESH_TOKEN_PROPERTY, newRefreshToken.getContent());
        accessTokenAdditional.put("expiresIn", DEFAULT_EXPIRY_IN_SECONDS);
        accessTokenAdditional.put("refreshEndpoint", refreshEndpoint);
        accessTokenAdditional.put("authType", "bearer");

        // the ClaimToken is created based solely on the TokenParameters. The additional information (refresh token...) is persisted separately
        var claimToken = ClaimToken.Builder.newInstance().claims(newTokenParams.getClaims()).build();
        var accessTokenData = new AccessTokenData(existingAccessTokenData.id(), claimToken, existingAccessTokenData.dataAddress(), accessTokenAdditional);

        var storeResult = accessTokenDataStore.update(accessTokenData);
        return storeResult.succeeded() ?
                Result.success(new TokenResponse(newAccessToken.getContent(),
                        newRefreshToken.getContent(), DEFAULT_EXPIRY_IN_SECONDS, "bearer")) :
                Result.failure(storeResult.getFailureMessages());
    }

    @Override
    public Result<TokenRepresentation> obtainToken(TokenParameters tokenParameters, DataAddress backendDataAddress, Map<String, Object> additionalTokenData) {
        Objects.requireNonNull(tokenParameters, "TokenParameters must be non-null.");
        Objects.requireNonNull(backendDataAddress, "DataAddress must be non-null.");


        //create a refresh token
        var refreshTokenResult = createToken(TokenParameters.Builder.newInstance().build());
        if (refreshTokenResult.failed()) {
            return Result.failure("Could not generate refresh token: %s".formatted(refreshTokenResult.getFailureDetail()));
        }

        var accessTokenResult = createToken(tokenParameters);
        if (accessTokenResult.failed()) {
            return Result.failure("Could not generate access token: %s".formatted(accessTokenResult.getFailureDetail()));
        }

        // additional token information will be added to the TokenRepresentation, which will be returned to the caller
        // note: can't use DBI (double-bracket initialization) here, because SonarCloud will complain about it
        var accessTokenAdditional = new HashMap<>(additionalTokenData);
        accessTokenAdditional.put("refreshToken", refreshTokenResult.getContent().tokenRepresentation().getToken());
        accessTokenAdditional.put("expiresIn", DEFAULT_EXPIRY_IN_SECONDS);
        accessTokenAdditional.put("refreshEndpoint", refreshEndpoint);
        accessTokenAdditional.put("authType", "bearer");

        var accessToken = TokenRepresentation.Builder.newInstance()
                .token(accessTokenResult.getContent().tokenRepresentation().getToken()) // the access token
                .additional(accessTokenAdditional) //contains additional properties and the refresh token
                .expiresIn(DEFAULT_EXPIRY_IN_SECONDS) //todo: needed?
                .build();

        // the ClaimToken is created based solely on the TokenParameters. The additional information (refresh token...) is persisted separately
        var claimToken = ClaimToken.Builder.newInstance().claims(tokenParameters.getClaims()).build();
        var accessTokenData = new AccessTokenData(accessTokenResult.getContent().id(), claimToken, backendDataAddress, accessTokenAdditional);

        var storeResult = accessTokenDataStore.store(accessTokenData);
        return storeResult.succeeded() ? Result.success(accessToken) : Result.failure(storeResult.getFailureMessages());
    }

    @Override
    public Result<AccessTokenData> resolve(String token) {
        return resolveToken(token, tokenValidationRules);
    }

    /**
     * Creates a token that has an ID based on the given token parameters. If the token parameters don't contain a "jti" claim, one
     * will be generated at random.
     */
    private Result<TokenRepresentationWithId> createToken(TokenParameters tokenParameters) {
        var claimDecorators = tokenParameters.getClaims().entrySet().stream().map(e -> (TokenDecorator) claimDecorator -> claimDecorator.claims(e.getKey(), e.getValue()));
        var headerDecorators = tokenParameters.getHeaders().entrySet().stream().map(e -> (TokenDecorator) headerDecorator -> headerDecorator.header(e.getKey(), e.getValue()));

        var tokenId = new AtomicReference<>(tokenParameters.getStringClaim(TOKEN_ID_CLAIM));
        var allDecorators = new ArrayList<>(Stream.concat(claimDecorators, headerDecorators).toList());

        // if there is no "jti" header on the token params, we'll assign a random one, and add it back to the decorators
        if (tokenId.get() == null) {
            monitor.info("No '%s' claim found on TokenParameters. Will generate a random one.".formatted(TOKEN_ID_CLAIM));
            tokenId.set(UUID.randomUUID().toString());
            TokenDecorator tokenIdDecorator = params -> params.claims(TOKEN_ID_CLAIM, tokenId.get());
            allDecorators.add(tokenIdDecorator);
        }

        return tokenGenerationService.generate(privateKeySupplier, allDecorators.toArray(new TokenDecorator[0]))
                .map(tr -> new TokenRepresentationWithId(tokenId.get(), tr));
    }

    /**
     * Parses the given token, and validates it against the given rules. For that, the publicKeyResolver is used.
     * Once the token is deemed valid, the "jti" claim (which is mandatory) is extracted and used as key for a lookup in the
     * {@link AccessTokenDataStore}. The result of that is then returned.
     */
    private Result<AccessTokenData> resolveToken(String token, List<TokenValidationRule> rules) {
        var validationResult = tokenValidationService.validate(token, publicKeyResolver, rules);
        if (validationResult.failed()) {
            return validationResult.mapTo();
        }
        var tokenId = validationResult.getContent().getStringClaim(TOKEN_ID_CLAIM);
        var existingAccessToken = accessTokenDataStore.getById(tokenId);

        return existingAccessToken == null ?
                Result.failure("AccessTokenData with ID '%s' does not exist.".formatted(tokenId)) :
                Result.success(existingAccessToken);
    }

    /**
     * container object for a TokenRepresentation and an ID
     */
    private record TokenRepresentationWithId(String id, TokenRepresentation tokenRepresentation) {

    }
}
