/*
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft
 * Copyright (c) 2026 SAP SE
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

import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.decentralizedclaims.spi.SecureTokenService;
import org.eclipse.edc.iam.decentralizedclaims.sts.remote.RemoteSecureTokenService;
import org.eclipse.edc.iam.decentralizedclaims.sts.remote.StsRemoteClientConfiguration;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.tractusx.edc.core.utils.PathUtils;
import org.eclipse.tractusx.edc.iam.dcp.sts.div.DivSecureTokenService;
import org.eclipse.tractusx.edc.iam.dcp.sts.div.oauth.DivOauth2Client;

import static java.util.Optional.ofNullable;

@Extension(RemoteTokenServiceClientExtension.NAME)
public class RemoteTokenServiceClientExtension implements ServiceExtension {

    @Setting(value = "STS Div endpoint")
    public static final String DIV_URL = "tx.edc.iam.sts.div.url";

    protected static final String NAME = "Secure Token Service (STS) client extension";

    @Inject
    private EdcHttpClient httpClient;
    @Inject
    private Monitor monitor;
    @Inject
    private TypeManager typeManager;
    @Inject
    private StsRemoteClientConfiguration clientConfiguration;
    @Inject
    private Oauth2Client oauth2Client;
    @Inject
    private Vault vault;
    @Inject
    private DivOauth2Client divOauth2Client;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public SecureTokenService secureTokenService(ServiceExtensionContext context) {
        var divUrlConfig = context.getSetting(DIV_URL, null);
        return ofNullable(divUrlConfig)
                .map(PathUtils::removeTrailingSlash)
                .map(divUrl -> {
                    monitor.info("DIV URL configured, will use DIV STS client");
                    return (SecureTokenService) new DivSecureTokenService(httpClient, divUrl, divOauth2Client, typeManager.getMapper(), monitor);
                })
                .orElseGet(() -> {
                    monitor.info("DIV URL not configured, will use the standard EDC Remote STS client");
                    return new RemoteSecureTokenService(oauth2Client, participantContextId -> clientConfiguration, vault);
                });
    }
}
