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

package org.eclipse.tractusx.edc.iam.dcp.sts;

import org.eclipse.edc.iam.decentralizedclaims.sts.remote.StsRemoteClientConfiguration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.tractusx.edc.core.utils.PathUtils;

/**
 * Configuration Extension for the STS OAuth2 client
 */
@Extension(StsClientConfigurationExtension.NAME)
public class StsClientConfigurationExtension implements ServiceExtension {

    static final String TOKEN_URL = "edc.iam.sts.oauth.token.url";
    static final String CLIENT_ID = "edc.iam.sts.oauth.client.id";
    static final String CLIENT_SECRET_ALIAS = "edc.iam.sts.oauth.client.secret.alias";

    @Setting(key = TOKEN_URL, description = "STS OAuth2 endpoint for requesting a token")
    private String tokenUrl;

    @Setting(key = CLIENT_ID, description = "STS OAuth2 client id")
    private String clientId;

    @Setting(key = CLIENT_SECRET_ALIAS, description = "Vault alias of STS OAuth2 client secret")
    private String clientSecretAlias;

    protected static final String NAME = "Secure Token Service (STS) client configuration extension";

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public StsRemoteClientConfiguration clientConfiguration(ServiceExtensionContext context) {
        return new StsRemoteClientConfiguration(PathUtils.removeTrailingSlash(tokenUrl), clientId, clientSecretAlias);
    }
}
