/*
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.edc.dataplane.tokenrefresh.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimNames;
import org.eclipse.edc.connector.dataplane.spi.AccessTokenData;
import org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAccessTokenService;
import org.eclipse.edc.connector.dataplane.spi.store.AccessTokenDataStore;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames;
import org.eclipse.edc.keys.spi.LocalPublicKeyService;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextSupplier;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.token.rules.ExpirationIssuedAtValidationRule;
import org.eclipse.edc.token.spi.KeyIdDecorator;
import org.eclipse.edc.token.spi.TokenDecorator;
import org.eclipse.edc.token.spi.TokenGenerationService;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.eclipse.tractusx.edc.dataplane.tokenrefresh.core.rules.AuthTokenAudienceRule;
import org.eclipse.tractusx.edc.dataplane.tokenrefresh.core.rules.ClaimIsPresentRule;
import org.eclipse.tractusx.edc.dataplane.tokenrefresh.core.rules.IssuerEqualsSubjectRule;
import org.eclipse.tractusx.edc.dataplane.tokenrefresh.core.rules.RefreshTokenValidationRule;
import org.eclipse.tractusx.edc.spi.tokenrefresh.dataplane.DataPlaneTokenRefreshService;
import org.eclipse.tractusx.edc.spi.tokenrefresh.dataplane.model.TokenResponse;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.EXPIRATION_TIME;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.AUDIENCE_PROPERTY;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.EDR_PROPERTY_EXPIRES_IN;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.EDR_PROPERTY_REFRESH_AUDIENCE;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.EDR_PROPERTY_REFRESH_ENDPOINT;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.EDR_PROPERTY_REFRESH_TOKEN;

/**
 * This implementation of the {@link DataPlaneTokenRefreshService} validates an incoming authentication token.
 */
public class DataPlaneTokenRefreshServiceImpl implements DataPlaneTokenRefreshService, DataPlaneAccessTokenService {
    public static final String ACCESS_TOKEN_CLAIM = "token";
    public static final String TOKEN_ID_CLAIM = "jti";
    private final long tokenExpirySeconds;
    private final List<TokenValidationRule> authenticationTokenValidationRules;
    private final ParticipantContextSupplier participantContextSupplier;
    private final List<TokenValidationRule> accessTokenAuthorizationRules;
    private final TokenValidationService tokenValidationService;
    private final DidPublicKeyResolver publicKeyResolver;
    private final LocalPublicKeyService localPublicKeyService;
    private final AccessTokenDataStore accessTokenDataStore;
    private final TokenGenerationService tokenGenerationService;
    private final Supplier<String> privateKeyIdSupplier;
    private final Supplier<String> publicKeyIdSupplier;
    private final Monitor monitor;
    private final String refreshEndpoint;
    private final String ownDid;
    private final Clock clock;
    private final Vault vault;
    private final ObjectMapper objectMapper;


    public DataPlaneTokenRefreshServiceImpl(Clock clock,
                                            TokenValidationService tokenValidationService,
                                            DidPublicKeyResolver publicKeyResolver,
                                            LocalPublicKeyService localPublicKeyService,
                                            AccessTokenDataStore accessTokenDataStore,
                                            TokenGenerationService tokenGenerationService,
                                            Supplier<String> privateKeyIdSupplier,
                                            Monitor monitor,
                                            String refreshEndpoint,
                                            String ownDid,
                                            int tokenExpiryToleranceSeconds,
                                            long tokenExpirySeconds,
                                            Supplier<String> publicKeyIdSupplier,
                                            Vault vault,
                                            ObjectMapper objectMapper,
                                            ParticipantContextSupplier participantContextSupplier) {
        this.tokenValidationService = tokenValidationService;
        this.publicKeyResolver = publicKeyResolver;
        this.localPublicKeyService = localPublicKeyService;
        this.accessTokenDataStore = accessTokenDataStore;
        this.tokenGenerationService = tokenGenerationService;
        this.privateKeyIdSupplier = privateKeyIdSupplier;
        this.monitor = monitor.withPrefix(getClass().getSimpleName());
        this.refreshEndpoint = refreshEndpoint;
        this.clock = clock;
        this.publicKeyIdSupplier = publicKeyIdSupplier;
        this.ownDid = ownDid;
        this.vault = vault;
        this.objectMapper = objectMapper;
        this.tokenExpirySeconds = tokenExpirySeconds;
        authenticationTokenValidationRules = List.of(new IssuerEqualsSubjectRule(),
                new ClaimIsPresentRule(AUDIENCE), // we don't check the contents, only it is present
                new ClaimIsPresentRule(ACCESS_TOKEN_CLAIM),
                new ClaimIsPresentRule(TOKEN_ID_CLAIM),
                new AuthTokenAudienceRule(accessTokenDataStore));
        this.participantContextSupplier = participantContextSupplier;
        accessTokenAuthorizationRules = List.of(new IssuerEqualsSubjectRule(),
                new ClaimIsPresentRule(AUDIENCE),
                new ClaimIsPresentRule(TOKEN_ID_CLAIM),
                new ExpirationIssuedAtValidationRule(clock, tokenExpiryToleranceSeconds, false));
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
     *     <li>assert the the token contains an {@code token} claim, and that the value is identical to the access token we have on record</li>
     *     <li>assert that the {@code refreshToken} parameter is identical to the refresh token we have on record</li>
     * </ul>
     *
     * @param refreshToken        The refresh token that was issued in the original/previous token request.
     * @param authenticationToken A <a href="https://github.com/eclipse-tractusx/tractusx-profiles/blob/main/tx/refresh/refresh.token.grant.profile.md#31-client-authentication">client authentication token</a>
     */
    @Override
    public Result<TokenResponse> refreshToken(String refreshToken, String authenticationToken) {

        authenticationToken = authenticationToken.replace("Bearer", "").trim();

        var authTokenRes = tokenValidationService.validate(authenticationToken, publicKeyResolver, authenticationTokenValidationRules);
        if (authTokenRes.failed()) {
            var msg = "Authentication token validation failed: %s".formatted(authTokenRes.getFailureDetail());
            monitor.severe(msg);
            return Result.failure(msg);
        }

        var participantContextServiceResult = participantContextSupplier.get();
        if (participantContextServiceResult.failed()) {
            var msg = "Cannot retrieve ParticipantContext: " + participantContextServiceResult.getFailureDetail();
            monitor.severe(msg);
            return Result.failure(msg);
        }
        var participantContext = participantContextServiceResult.getContent();

        // 2. extract access token and validate it
        var accessToken = authTokenRes.getContent().getStringClaim("token");
        var refreshTokenValidationRule = new RefreshTokenValidationRule(vault, refreshToken, objectMapper, participantContext);
        var accessTokenDataResult = tokenValidationService.validate(accessToken, localPublicKeyService, refreshTokenValidationRule)
                .map(accessTokenClaims -> accessTokenDataStore.getById(accessTokenClaims.getStringClaim(JwtRegisteredClaimNames.JWT_ID)));

        if (accessTokenDataResult.failed()) {
            var msg = "Access token validation failed: %s".formatted(accessTokenDataResult.getFailureDetail());
            monitor.severe(msg);
            return Result.failure(msg);
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
            var msg = "Failed to regenerate access/refresh token pair: %s".formatted(errors);
            monitor.severe(msg);
            return Result.failure(msg);
        }

        storeRefreshToken(existingAccessTokenData.id(), new RefreshToken(newRefreshToken.getContent(), tokenExpirySeconds, refreshEndpoint), participantContext);

        // the ClaimToken is created based solely on the TokenParameters. The additional information (refresh token...) is persisted separately
        var claimToken = ClaimToken.Builder.newInstance().claims(newTokenParams.getClaims()).build();
        var accessTokenData = new AccessTokenData(existingAccessTokenData.id(), claimToken, existingAccessTokenData.dataAddress(), existingAccessTokenData.additionalProperties());

        var storeResult = accessTokenDataStore.update(accessTokenData);

        if (storeResult.failed()) {
            monitor.severe("Failed to store refreshed access token data: %s".formatted(storeResult.getFailureDetail()));
            return Result.failure(storeResult.getFailureMessages());
        }
        return Result.success(new TokenResponse(newAccessToken.getContent(), newRefreshToken.getContent(), tokenExpirySeconds, "bearer"));
    }

    @Override
    public Result<TokenRepresentation> obtainToken(TokenParameters tokenParameters, DataAddress backendDataAddress, Map<String, Object> additionalTokenData) {
        Objects.requireNonNull(tokenParameters, "TokenParameters must be non-null.");
        Objects.requireNonNull(backendDataAddress, "DataAddress must be non-null.");


        //create a refresh token
        var refreshTokenResult = createToken(TokenParameters.Builder.newInstance().build());
        if (refreshTokenResult.failed()) {
            var msg = "Could not generate refresh token: %s".formatted(refreshTokenResult.getFailureDetail());
            monitor.severe(msg);
            return Result.failure(msg);
        }

        var accessTokenResult = createToken(tokenParameters);
        if (accessTokenResult.failed()) {
            var msg = "Could not generate access token: %s".formatted(accessTokenResult.getFailureDetail());
            monitor.severe(msg);
            return Result.failure(msg);
        }

        // the edrAdditionalData contains the refresh token, which is NOT supposed to be put in the DB
        // note: can't use DBI (double-bracket initialization) here, because SonarCloud will complain about it
        var additionalDataForStorage = new HashMap<>(additionalTokenData);
        additionalDataForStorage.put("authType", "bearer");

        // the ClaimToken is created based solely on the TokenParameters. The additional information (refresh token...) is persisted separately
        var claimToken = ClaimToken.Builder.newInstance().claims(tokenParameters.getClaims()).build();
        var accessTokenData = new AccessTokenData(accessTokenResult.getContent().id(), claimToken, backendDataAddress, additionalDataForStorage);
        var storeResult = accessTokenDataStore.store(accessTokenData);

        var participantContextServiceResult = participantContextSupplier.get();
        if (participantContextServiceResult.failed()) {
            var msg = "Cannot retrieve ParticipantContext: " + participantContextServiceResult.getFailureDetail();
            monitor.severe(msg);
            return Result.failure(msg);
        }
        var participantContext = participantContextServiceResult.getContent();

        storeRefreshToken(accessTokenResult.getContent().id(), new RefreshToken(refreshTokenResult.getContent().tokenRepresentation().getToken(),
                tokenExpirySeconds, refreshEndpoint), participantContext);

        // the refresh token information must be returned in the EDR
        var audience = additionalDataForStorage.get(AUDIENCE_PROPERTY);

        if (audience == null) {
            var msg = "Missing audience in the additional properties";
            monitor.severe(msg);
            return Result.failure(msg);
        }

        var edrAdditionalData = new HashMap<>(additionalTokenData);
        edrAdditionalData.put(EDR_PROPERTY_REFRESH_TOKEN, refreshTokenResult.getContent().tokenRepresentation().getToken());
        edrAdditionalData.put(EDR_PROPERTY_EXPIRES_IN, String.valueOf(tokenExpirySeconds));
        edrAdditionalData.put(EDR_PROPERTY_REFRESH_ENDPOINT, refreshEndpoint);
        edrAdditionalData.put(EDR_PROPERTY_REFRESH_AUDIENCE, audience);

        var edrTokenRepresentation = TokenRepresentation.Builder.newInstance()
                .token(accessTokenResult.getContent().tokenRepresentation().getToken()) // the access token
                .additional(edrAdditionalData) //contains additional properties and the refresh token
                .expiresIn(tokenExpirySeconds) //todo: needed?
                .build();

        if (storeResult.failed()) {
            monitor.severe("Could not store AccessTokenData: %s".formatted(storeResult.getFailureDetail()));
            return Result.failure(storeResult.getFailureMessages());
        }

        return Result.success(edrTokenRepresentation);
    }

    @Override
    public Result<AccessTokenData> resolve(String token) {
        return tokenValidationService.validate(token, localPublicKeyService, accessTokenAuthorizationRules)
                .compose(claimToken -> {
                    var id = claimToken.getStringClaim(JWTClaimNames.JWT_ID);
                    var tokenData = accessTokenDataStore.getById(id);

                    if (tokenData == null) {
                        var msg = "AccessTokenData with ID '%s' does not exist.".formatted(id);
                        monitor.warning(msg);
                        return  Result.failure(msg);
                    }
                    return Result.success(tokenData);
                });
    }

    @Override
    public ServiceResult<Void> revoke(String transferProcessId, String reason) {
        var query = QuerySpec.Builder.newInstance()
                .filter(new Criterion("additionalProperties.process_id", "=", transferProcessId))
                .build();

        var tokens = accessTokenDataStore.query(query);
        return tokens.stream().map(this::deleteTokenData)
                .reduce(Result::merge)
                .map(ServiceResult::from)
                .orElseGet(() -> {
                    var msg = "AccessTokenData associated to the transfer with ID '%s' does not exist.".formatted(transferProcessId);
                    monitor.warning(msg);
                    return ServiceResult.notFound(msg);
                });
    }

    private Result<Void> deleteTokenData(AccessTokenData tokenData) {
        var deletionResult = vault.deleteSecret(tokenData.id());
        if (deletionResult.failed()) {
            return deletionResult;
        }

        var result = accessTokenDataStore.deleteById(tokenData.id());
        if (result.failed()) {
            return Result.failure(result.getFailureDetail());
        } else {
            return Result.success();
        }
    }

    /**
     * Creates a token that has an ID based on the given token parameters. If the token parameters don't contain a "jti" claim, one
     * will be generated at random.
     */
    private ServiceResult<TokenRepresentationWithId> createToken(TokenParameters tokenParameters) {
        var claims = new HashMap<>(tokenParameters.getClaims());
        claims.put(JwtRegisteredClaimNames.ISSUED_AT, clock.instant().getEpochSecond()); // iat is millis in upstream -> bug
        var claimDecorators = claims.entrySet().stream().map(e -> (TokenDecorator) claimDecorator -> claimDecorator.claims(e.getKey(), e.getValue()));
        var headerDecorators = tokenParameters.getHeaders().entrySet().stream().map(e -> (TokenDecorator) headerDecorator -> headerDecorator.header(e.getKey(), e.getValue()));

        var tokenId = new AtomicReference<>(tokenParameters.getStringClaim(TOKEN_ID_CLAIM));
        var allDecorators = new ArrayList<>(Stream.concat(claimDecorators, headerDecorators).toList());
        allDecorators.add(new KeyIdDecorator(publicKeyIdSupplier.get()));

        // if there is no "jti" header on the token params, we'll assign a random one, and add it back to the decorators
        if (tokenId.get() == null) {
            monitor.info("No '%s' claim found on TokenParameters. Will generate a random one.".formatted(TOKEN_ID_CLAIM));
            tokenId.set(UUID.randomUUID().toString());
            TokenDecorator tokenIdDecorator = params -> params.claims(TOKEN_ID_CLAIM, tokenId.get());
            allDecorators.add(tokenIdDecorator);
        }
        //if there is not "exp" header on the token params, we'll configure one
        if (!tokenParameters.getClaims().containsKey(JwtRegisteredClaimNames.EXPIRATION_TIME)) {
            monitor.info("No '%s' claim found on TokenParameters. Will use the configured default of %d seconds".formatted(EXPIRATION_TIME, tokenExpirySeconds));
            var exp = clock.instant().plusSeconds(tokenExpirySeconds).getEpochSecond();
            allDecorators.add(tp -> tp.claims(JwtRegisteredClaimNames.EXPIRATION_TIME, exp));
        }

        return participantContextSupplier.get().map(ParticipantContext::getParticipantContextId)
                .compose(participantContextId -> tokenGenerationService.generate(participantContextId,
                        privateKeyIdSupplier.get(), allDecorators.toArray(new TokenDecorator[0])).flatMap(ServiceResult::from))
                .map(tr -> new TokenRepresentationWithId(tokenId.get(), tr));
    }

    private Result<Void> storeRefreshToken(String id, RefreshToken refreshToken, ParticipantContext participantContext) {
        return toJson(refreshToken)
                .compose(json -> vault.storeSecret(participantContext.getParticipantContextId(), id, json));
    }

    private Result<String> toJson(Object object) {
        try {
            return Result.success(objectMapper.writeValueAsString(object));
        } catch (JsonProcessingException e) {
            return Result.failure(e.getMessage());
        }
    }

    /**
     * container object for a TokenRepresentation and an ID
     */
    private record TokenRepresentationWithId(String id, TokenRepresentation tokenRepresentation) {

    }

}
