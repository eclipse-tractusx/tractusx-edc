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

import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.identitytrust.SecureTokenService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.tractusx.edc.core.utils.PathUtils;
import org.eclipse.tractusx.edc.iam.iatp.sts.dim.oauth.DimOauth2Client;

import static java.util.Optional.ofNullable;
import static org.eclipse.tractusx.edc.core.utils.RequiredConfigWarnings.warningNotPresent;

@Extension(DimSecureTokenServiceExtension.NAME)
public class DimSecureTokenServiceExtension implements ServiceExtension {

    @Setting(value = "STS Dim endpoint")
    public static final String DIM_URL = "edc.iam.sts.dim.url";
    protected static final String NAME = "DIM Secure token service extension";

    @Inject
    private StsRemoteClientConfiguration stsRemoteClientConfiguration;

    @Inject
    private DimOauth2Client dimOauth2Client;

    @Inject
    private EdcHttpClient httpClient;

    @Inject
    private Monitor monitor;

    @Inject
    private TypeManager typeManager;


    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public SecureTokenService secureTokenService(ServiceExtensionContext context) {
        var dimUrl = ofNullable(context.getConfig().getString(DIM_URL, null))
                .map(PathUtils::removeTrailingSlash)
                .orElse(null);

        if (dimUrl == null) {
            warningNotPresent(context.getMonitor().withPrefix("STS Client for DIM"), DIM_URL);
        }

        return new DimSecureTokenService(httpClient, dimUrl, dimOauth2Client, typeManager.getMapper(), monitor);
    }
}