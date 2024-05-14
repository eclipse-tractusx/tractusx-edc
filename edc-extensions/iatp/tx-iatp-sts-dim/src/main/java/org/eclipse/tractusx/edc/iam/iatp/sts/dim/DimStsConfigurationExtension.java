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

package org.eclipse.tractusx.edc.iam.iatp.sts.dim;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.tractusx.edc.core.utils.PathUtils;

import static org.eclipse.tractusx.edc.core.utils.ConfigUtil.propertyCompatibility;

/**
 * Configuration Extension for the STS OAuth2 client
 */
@Extension(DimStsConfigurationExtension.NAME)
public class DimStsConfigurationExtension implements ServiceExtension {

    @Setting(value = "STS OAuth2 endpoint for requesting a token")
    public static final String TOKEN_URL = "tx.edc.iam.sts.oauth.token.url";
    @Deprecated(since = "0.7.1")
    public static final String TOKEN_URL_DEPRECATED = "edc.iam.sts.oauth.token.url";

    @Setting(value = "STS OAuth2 client id")
    public static final String CLIENT_ID = "tx.edc.iam.sts.oauth.client.id";
    @Deprecated(since = "0.7.1")
    public static final String CLIENT_ID_DEPRECATED = "edc.iam.sts.oauth.client.id";

    @Setting(value = "Vault alias of STS OAuth2 client secret")
    public static final String CLIENT_SECRET_ALIAS = "tx.edc.iam.sts.oauth.client.secret.alias";
    @Deprecated(since = "0.7.1")
    public static final String CLIENT_SECRET_ALIAS_DEPRECATED = "edc.iam.sts.oauth.client.secret.alias";

    protected static final String NAME = "DIM STS client configuration extension";


    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public StsRemoteClientConfiguration clientConfiguration(ServiceExtensionContext context) {

        var tokenUrl = PathUtils.removeTrailingSlash(propertyCompatibility(context, TOKEN_URL, TOKEN_URL_DEPRECATED));
        var clientId = propertyCompatibility(context, CLIENT_ID, CLIENT_ID_DEPRECATED);
        var clientSecretAlias = propertyCompatibility(context, CLIENT_SECRET_ALIAS, CLIENT_SECRET_ALIAS_DEPRECATED);

        var monitor = context.getMonitor().withPrefix("STS Client for DIM");

        return new StsRemoteClientConfiguration(tokenUrl, clientId, clientSecretAlias);
    }

}
