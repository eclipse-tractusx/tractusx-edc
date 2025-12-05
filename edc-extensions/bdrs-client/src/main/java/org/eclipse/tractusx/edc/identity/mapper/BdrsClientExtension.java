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

package org.eclipse.tractusx.edc.identity.mapper;

import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.decentralizedclaims.service.DidCredentialServiceUrlResolver;
import org.eclipse.edc.iam.decentralizedclaims.spi.CredentialServiceClient;
import org.eclipse.edc.iam.decentralizedclaims.spi.SecureTokenService;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;

import java.util.function.Supplier;

import static org.eclipse.tractusx.edc.core.utils.ConfigUtil.missingMandatoryProperty;
import static org.eclipse.tractusx.edc.identity.mapper.BdrsClientExtension.NAME;

@Extension(value = NAME)
public class BdrsClientExtension implements ServiceExtension {
    public static final String NAME = "BPN/DID Resolution Service Client Extension";

    public static final int DEFAULT_BDRS_CACHE_VALIDITY = 15 * 60; // 15 minutes
    @Setting(value = "Base URL of the BDRS service", required = true)
    public static final String BDRS_SERVER_URL_PROPERTY = "tx.edc.iam.iatp.bdrs.server.url";

    @Setting(value = "Base URL of the CredentialService, that belongs to this connector runtime. If not specified, the URL is resolved from this participant's DID document.")
    public static final String CREDENTIAL_SERVICE_BASE_URL_PROPERTY = "tx.edc.iam.iatp.credentialservice.url";

    @Setting(value = "Validity period in seconds for the cached BPN/DID mappings. After this period a new resolution request will hit the server.", defaultValue = DEFAULT_BDRS_CACHE_VALIDITY + "")
    public static final String BDRS_SERVER_CACHE_VALIDITY_PERIOD = "tx.edc.iam.iatp.bdrs.cache.validity";

    // this setting is already defined in IdentityAndTrustExtension
    public static final String CONNECTOR_DID_PROPERTY = "edc.iam.issuer.id";

    @Inject
    private EdcHttpClient httpClient;
    @Inject
    private TypeManager typeManager;
    @Inject
    private SecureTokenService secureTokenService;
    @Inject
    private CredentialServiceClient credentialServiceClient;
    @Inject
    private DidResolverRegistry didResolverRegistry;
    @Inject
    private SingleParticipantContextSupplier participantContextSupplier;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public BdrsClient getBdrsClient(ServiceExtensionContext context) {
        var baseUrl = context.getConfig().getString(BDRS_SERVER_URL_PROPERTY);
        var monitor = context.getMonitor();
        var cacheValidity = context.getSetting(BDRS_SERVER_CACHE_VALIDITY_PERIOD, DEFAULT_BDRS_CACHE_VALIDITY);

        // get DID
        var ownDid = context.getConfig().getString(CONNECTOR_DID_PROPERTY, null);
        if (ownDid == null) {
            missingMandatoryProperty(monitor, CONNECTOR_DID_PROPERTY);
        }

        // get CS URL
        Supplier<String> urlSupplier;
        var configuredUrl = context.getSetting(CREDENTIAL_SERVICE_BASE_URL_PROPERTY, null);
        if (configuredUrl != null) {
            urlSupplier = () -> configuredUrl;
        } else {
            monitor.warning("No config value found for '%s'. As a fallback, the credentialService URL from this connector's DID document will be resolved.".formatted(CREDENTIAL_SERVICE_BASE_URL_PROPERTY));

            urlSupplier = () -> {
                var resolver = new DidCredentialServiceUrlResolver(didResolverRegistry);
                return resolver.resolve(ownDid).orElse(f -> {
                    monitor.severe("Resolving the credentialService URL failed. This runtime won't be able to communicate with BDRS. Error: %s.".formatted(f.getFailureDetail()));
                    return null;
                });
            };

        }

        return new BdrsClientImpl(baseUrl, cacheValidity, ownDid, urlSupplier, httpClient, monitor, typeManager.getMapper(),
                secureTokenService, credentialServiceClient, participantContextSupplier);
    }

}
