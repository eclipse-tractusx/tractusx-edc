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

import static org.eclipse.tractusx.edc.core.utils.PathUtils.removeTrailingSlash;

/**
 * Configuration Extension for the STS OAuth2 client
 */
@Extension(DimStsConfigurationExtension.NAME)
public class DimStsConfigurationExtension implements ServiceExtension {

    @Setting(value = "STS OAuth2 endpoint for requesting a token")
    public static final String TOKEN_URL = "edc.iam.sts.oauth.token.url";

    @Setting(value = "STS OAuth2 client id")
    public static final String CLIENT_ID = "edc.iam.sts.oauth.client.id";

    @Setting(value = "Vault alias of STS OAuth2 client secret")
    public static final String CLIENT_SECRET_ALIAS = "edc.iam.sts.oauth.client.secret.alias";

    protected static final String NAME = "DIM STS client configuration extension";

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public StsRemoteClientConfiguration clientConfiguration(ServiceExtensionContext context) {

        var tokenUrl = removeTrailingSlash(context.getConfig().getString(TOKEN_URL));
        var clientId = context.getConfig().getString(CLIENT_ID);
        var clientSecretAlias = context.getConfig().getString(CLIENT_SECRET_ALIAS);

        return new StsRemoteClientConfiguration(tokenUrl, clientId, clientSecretAlias);
    }

}
