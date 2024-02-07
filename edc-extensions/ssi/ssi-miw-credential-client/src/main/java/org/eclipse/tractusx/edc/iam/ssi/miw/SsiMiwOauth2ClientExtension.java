/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.iam.ssi.miw;

import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.tractusx.edc.iam.ssi.miw.oauth2.MiwOauth2Client;
import org.eclipse.tractusx.edc.iam.ssi.miw.oauth2.MiwOauth2ClientConfiguration;
import org.eclipse.tractusx.edc.iam.ssi.miw.oauth2.MiwOauth2ClientImpl;

import java.util.Objects;

import static org.eclipse.tractusx.edc.iam.ssi.miw.utils.PathUtils.removeTrailingSlash;


@Extension(SsiMiwOauth2ClientExtension.EXTENSION_NAME)
public class SsiMiwOauth2ClientExtension implements ServiceExtension {

    public static final String EXTENSION_NAME = "SSI MIW OAuth2 Client";

    @Setting(value = "OAuth2 endpoint for requesting a token")
    public static final String TOKEN_URL = "tx.ssi.oauth.token.url";


    @Setting(value = "OAuth2 client id")
    public static final String CLIENT_ID = "tx.ssi.oauth.client.id";

    @Setting(value = "Vault alias of OAuth2 client secret")
    public static final String CLIENT_SECRET_ALIAS = "tx.ssi.oauth.client.secret.alias";

    @Inject
    private Oauth2Client oauth2Client;

    @Inject
    private Vault vault;

    @Override
    public String name() {
        return EXTENSION_NAME;
    }

    @Provider
    public MiwOauth2Client oauth2Client(ServiceExtensionContext context) {
        return new MiwOauth2ClientImpl(oauth2Client, createConfiguration(context));
    }

    private MiwOauth2ClientConfiguration createConfiguration(ServiceExtensionContext context) {
        var tokenUrl = removeTrailingSlash(context.getConfig().getString(TOKEN_URL));
        var clientId = context.getConfig().getString(CLIENT_ID);
        var clientSecretAlias = context.getConfig().getString(CLIENT_SECRET_ALIAS);
        var clientSecret = vault.resolveSecret(clientSecretAlias);
        Objects.requireNonNull(clientSecret, "Client secret could not be retrieved");

        return MiwOauth2ClientConfiguration.Builder.newInstance()
                .tokenUrl(tokenUrl)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();
    }
}
