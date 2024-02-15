/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
 ********************************************************************************/

package org.eclipse.tractusx.edc.iam.iatp.sts.dim.oauth;

import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2CredentialsRequest;
import org.eclipse.edc.iam.oauth2.spi.client.SharedSecretOauth2CredentialsRequest;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.tractusx.edc.iam.iatp.sts.dim.StsRemoteClientConfiguration;
import org.jetbrains.annotations.NotNull;

public class DimOauthClientImpl implements DimOauth2Client {

    private static final String GRANT_TYPE = "client_credentials";
    private final StsRemoteClientConfiguration configuration;
    private final Oauth2Client oauth2Client;

    private final Vault vault;

    public DimOauthClientImpl(Oauth2Client oauth2Client, Vault vault, StsRemoteClientConfiguration configuration) {
        this.configuration = configuration;
        this.oauth2Client = oauth2Client;
        this.vault = vault;
    }

    @Override
    public Result<TokenRepresentation> obtainRequestToken() {
        return createRequest().compose(oauth2Client::requestToken);

    }
    
    @NotNull
    private Result<Oauth2CredentialsRequest> createRequest() {
        var secret = vault.resolveSecret(configuration.clientSecretAlias());
        if (secret != null) {
            var builder = SharedSecretOauth2CredentialsRequest.Builder.newInstance()
                    .url(configuration.tokenUrl())
                    .clientId(configuration.clientId())
                    .clientSecret(secret)
                    .grantType(GRANT_TYPE);

            return Result.success(builder.build());
        } else {
            return Result.failure("Failed to fetch client secret from the vault with alias: %s".formatted(configuration.clientSecretAlias()));
        }
    }
}
