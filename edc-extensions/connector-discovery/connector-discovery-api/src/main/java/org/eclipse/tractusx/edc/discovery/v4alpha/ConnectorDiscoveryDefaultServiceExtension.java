/*
 * Copyright (c) 2025 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 * Copyright (c) 2026 Cofinity-X GmbH
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

package org.eclipse.tractusx.edc.discovery.v4alpha;

import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.tractusx.edc.discovery.v4alpha.service.ConnectorDiscoveryServiceImpl;
import org.eclipse.tractusx.edc.discovery.v4alpha.service.DidMapper;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorDiscoveryService;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.DspVersionToIdentifierMapper;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.IdentifierToDidMapper;

import java.time.Clock;

import static org.eclipse.tractusx.edc.discovery.v4alpha.ConnectorDiscoveryExtension.NAME;

@Extension(value = NAME)
public class ConnectorDiscoveryDefaultServiceExtension implements ServiceExtension {

    public static final String NAME = "Default Connector Discovery Service Extension";

    public static final String TX_EDC_CONNECTOR_DISCOVERY_CACHE_EXPIRY = "tx.edc.connector.discovery.cache.expiry";

    @Override
    public String name() {
        return NAME;
    }

    @Setting(description = "Expiry time for caching protocol version information in milliseconds", key = TX_EDC_CONNECTOR_DISCOVERY_CACHE_EXPIRY, defaultValue = 1000 * 60 * 12 + "")
    private long didCacheExpiryMillis;

    @Inject
    private DidResolverRegistry didResolver;
    @Inject
    private Monitor monitor;
    @Inject
    private EdcHttpClient httpClient;
    @Inject
    private TypeManager typeManager;
    @Inject
    private IdentifierToDidMapper identifierMapper;
    @Inject
    private DspVersionToIdentifierMapper dspVersionMapper;
    @Inject
    private Clock clock;

    @Provider(isDefault = true)
    public ConnectorDiscoveryService defaultConnectorDiscoveryService() {
        return new ConnectorDiscoveryServiceImpl(didResolver, httpClient, typeManager.getMapper(), identifierMapper, dspVersionMapper, clock, didCacheExpiryMillis, monitor);
    }

    @Provider(isDefault = true)
    public IdentifierToDidMapper defaultIdentityMapper() {
        return new DidMapper();
    }

    @Provider(isDefault = true)
    public DspVersionToIdentifierMapper defaultDspVersionToIdentifierMapper() {
        return new DspVersionToIdentifierMapper() { };
    }
}