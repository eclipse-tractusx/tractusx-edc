/********************************************************************************
 * Copyright (c) 2025 Cofinity-X GmbH
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

package org.eclipse.tractusx.edc.interceptor;

import okhttp3.OkHttpClient;
import org.eclipse.edc.iam.identitytrust.sts.remote.StsRemoteClientConfiguration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.tractusx.edc.identity.mapper.BdrsClientExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.eclipse.edc.iam.identitytrust.core.defaults.DefaultCredentialServiceClient.PRESENTATION_ENDPOINT;
import static org.eclipse.tractusx.edc.core.utils.ConfigUtil.propertyCompatibility;

@Extension("Okhttp interceptors Extension")
public class OkHttpInterceptorExtension implements ServiceExtension {

    @Inject
    private OkHttpClient okHttpClient;

    @Inject
    private StsRemoteClientConfiguration clientConfiguration;

    @Provider
    public OkHttpClient okHttpClient(ServiceExtensionContext context) {
        List<String> skipPaths = new ArrayList<>(Arrays.asList(
                "/v1/dataflows",
                PRESENTATION_ENDPOINT,
                "/did.json"));

        var bdrsServerUrl = propertyCompatibility(context,
                BdrsClientExtension.BDRS_SERVER_URL_PROPERTY, BdrsClientExtension.BDRS_SERVER_URL_PROPERTY_DEPRECATED);
        var credentialServiceUrl = propertyCompatibility(context,
                BdrsClientExtension.CREDENTIAL_SERVICE_BASE_URL_PROPERTY, BdrsClientExtension.CREDENTIAL_SERVICE_BASE_URL_PROPERTY_DEPRECATED, null);

        addSkipPath(bdrsServerUrl, skipPaths);
        addSkipPath(credentialServiceUrl, skipPaths);
        addSkipPath(clientConfiguration.tokenUrl(), skipPaths);

        return okHttpClient.newBuilder().addInterceptor(new OkHttpInterceptor(skipPaths)).build();
    }

    private void addSkipPath(String path, List<String> skipPaths) {
        if (path != null && !path.isBlank()) {
            skipPaths.add(path);
        }
    }
}
