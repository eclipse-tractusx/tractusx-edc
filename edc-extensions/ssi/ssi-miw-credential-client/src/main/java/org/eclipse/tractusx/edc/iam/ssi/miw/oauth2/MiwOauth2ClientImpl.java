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

package org.eclipse.tractusx.edc.iam.ssi.miw.oauth2;

import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2CredentialsRequest;
import org.eclipse.edc.iam.oauth2.spi.client.SharedSecretOauth2CredentialsRequest;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;

public class MiwOauth2ClientImpl implements MiwOauth2Client {

    private static final String GRANT_TYPE = "client_credentials";
    private final Oauth2Client oauth2Client;

    private final MiwOauth2ClientConfiguration configuration;

    public MiwOauth2ClientImpl(Oauth2Client oauth2Client, MiwOauth2ClientConfiguration configuration) {
        this.oauth2Client = oauth2Client;
        this.configuration = configuration;
    }

    @Override
    public Result<TokenRepresentation> obtainRequestToken() {
        return oauth2Client.requestToken(createRequest());
    }

    public MiwOauth2ClientConfiguration getConfiguration() {
        return configuration;
    }

    @NotNull
    private Oauth2CredentialsRequest createRequest() {
        var builder = SharedSecretOauth2CredentialsRequest.Builder.newInstance()
                .url(configuration.getTokenUrl())
                .clientId(configuration.getClientId())
                .clientSecret(configuration.getClientSecret())
                .grantType(GRANT_TYPE);

        if (configuration.getScope() != null) {
            builder.scope(configuration.getScope());
        }
        return builder.build();
    }
}
