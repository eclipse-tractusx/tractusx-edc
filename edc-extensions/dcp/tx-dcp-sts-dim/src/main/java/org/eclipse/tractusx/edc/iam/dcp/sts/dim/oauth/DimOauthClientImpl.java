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

package org.eclipse.tractusx.edc.iam.dcp.sts.dim.oauth;

import org.eclipse.edc.iam.decentralizedclaims.sts.remote.StsRemoteClientConfiguration;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2CredentialsRequest;
import org.eclipse.edc.iam.oauth2.spi.client.SharedSecretOauth2CredentialsRequest;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextSupplier;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public class DimOauthClientImpl implements DimOauth2Client {

    private static final String GRANT_TYPE = "client_credentials";
    private final StsRemoteClientConfiguration configuration;
    private final Oauth2Client oauth2Client;

    private final Vault vault;
    private final Clock clock;
    private final Monitor monitor;
    private final ParticipantContextSupplier participantContextSupplier;

    private volatile TimestampedToken authToken;

    public DimOauthClientImpl(Oauth2Client oauth2Client, Vault vault, StsRemoteClientConfiguration configuration, Clock clock,
                              Monitor monitor, ParticipantContextSupplier participantContextSupplier) {
        this.configuration = configuration;
        this.oauth2Client = oauth2Client;
        this.vault = vault;
        this.clock = clock;
        this.monitor = monitor.withPrefix(getClass().getSimpleName());
        this.participantContextSupplier = participantContextSupplier;
    }

    @Override
    public Result<TokenRepresentation> obtainRequestToken() {
        if (isExpired()) {
            synchronized (this) {
                if (isExpired()) {
                    monitor.debug("DIM Token expired, need to refresh.");
                    // expiresIn should always be present, but if not we don't cache it
                    return requestToken().onSuccess(tokenRepresentation -> Optional.ofNullable(tokenRepresentation.getExpiresIn())
                            .ifPresent(expiresIn -> this.authToken = new TimestampedToken(tokenRepresentation, Instant.now(clock), expiresIn)));
                } else {
                    return Result.success(authToken.value);
                }
            }
        } else {
            return Result.success(authToken.value);
        }
    }

    private Result<TokenRepresentation> requestToken() {
        return createRequest().compose(oauth2Client::requestToken);
    }

    private boolean isExpired() {
        if (authToken == null) {
            return true;
        }
        return authToken.isExpired(clock);
    }

    @NotNull
    private Result<Oauth2CredentialsRequest> createRequest() {
        var participantContextServiceResult = participantContextSupplier.get();
        if (participantContextServiceResult.failed()) {
            var msg = "Cannot retrieve Participant Context";
            monitor.severe(msg + ": " + participantContextServiceResult.getFailureDetail());
            return Result.failure(msg);
        }

        var secret = vault.resolveSecret(participantContextServiceResult.getContent().getParticipantContextId(), configuration.clientSecretAlias());
        if (secret != null) {
            var builder = SharedSecretOauth2CredentialsRequest.Builder.newInstance()
                    .url(configuration.tokenUrl())
                    .clientId(configuration.clientId())
                    .clientSecret(secret)
                    .grantType(GRANT_TYPE);

            return Result.success(builder.build());
        } else {
            var msg = "Failed to fetch client secret from the vault with alias: %s".formatted(configuration.clientSecretAlias());
            monitor.severe(msg);
            return Result.failure(msg);
        }
    }

    record TimestampedToken(TokenRepresentation value, Instant lastUpdatedAt, long validitySeconds) {

        public boolean isExpired(Clock clock) {
            return lastUpdatedAt.plus(validitySeconds, ChronoUnit.SECONDS).isBefore(Instant.now(clock));
        }
    }
}
