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

import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.identitytrust.spi.SecureTokenService;
import org.eclipse.edc.iam.identitytrust.sts.remote.RemoteSecureTokenService;
import org.eclipse.edc.iam.identitytrust.sts.remote.StsRemoteClientConfiguration;
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
import org.eclipse.tractusx.edc.iam.dcp.sts.dim.DimSecureTokenService;
import org.eclipse.tractusx.edc.iam.dcp.sts.dim.oauth.DimOauth2Client;
import org.eclipse.tractusx.edc.iam.dcp.sts.dim.oauth.DimOauthClientImpl;

import java.time.Clock;

import static java.util.Optional.ofNullable;

@Extension(RemoteTokenServiceClientExtension.NAME)
public class RemoteTokenServiceClientExtension implements ServiceExtension {

    @Setting(value = "STS Dim endpoint")
    public static final String DIM_URL = "tx.edc.iam.sts.dim.url";
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
    private Clock clock;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public SecureTokenService secureTokenService(ServiceExtensionContext context) {
        var dimUrlConfig = context.getSetting(DIM_URL, null);
        return ofNullable(dimUrlConfig)
                .map(PathUtils::removeTrailingSlash)
                .map(dimUrl -> {
                    monitor.info("DIM URL configured, will use DIM STS client");
                    return (SecureTokenService) new DimSecureTokenService(httpClient, dimUrl, oauth2Client(), typeManager.getMapper(), monitor);
                })
                .orElseGet(() -> {
                    monitor.info("DIM URL not configured, will use the standard EDC Remote STS client");
                    return new RemoteSecureTokenService(oauth2Client, clientConfiguration, vault);
                });
    }

    private DimOauth2Client oauth2Client() {
        return new DimOauthClientImpl(oauth2Client, vault, clientConfiguration, clock, monitor);
    }

}