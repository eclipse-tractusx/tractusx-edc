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

package org.eclipse.tractusx.edc.discovery.cx;

import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.tractusx.edc.discovery.cx.service.BpnlAndDsp08ConnectorDiscoveryServiceImpl;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.CacheConfig;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorDiscoveryService;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;

import java.time.Clock;

import static org.eclipse.tractusx.edc.discovery.v4alpha.ConnectorDiscoveryExtension.NAME;
import static org.eclipse.tractusx.edc.discovery.v4alpha.ConnectorDiscoveryExtension.TX_EDC_CONNECTOR_DISCOVERY_CACHE_EXPIRY;

@Extension(value = NAME)
public class ConnectorDiscoveryBpnlAndDsp08ServiceExtension implements ServiceExtension {

    public static final String NAME = "Default Connector Discovery Service Extension";

    @Override
    public String name() {
        return NAME;
    }

    @Inject
    private BdrsClient bdrsClient;
    @Inject
    private TypeManager typeManager;
    @Inject
    private DidResolverRegistry didResolver;
    @Inject
    private EdcHttpClient httpClient;
    @Inject
    private Clock clock;
    @Inject
    private Monitor monitor;

    @Setting(description = "Expiry time for caching protocol version information in milliseconds",
            key = TX_EDC_CONNECTOR_DISCOVERY_CACHE_EXPIRY, defaultValue = 1000 * 60 * 120 + "")
    private long connectorDiscoveryCacheExpiry;

    @Provider
    public ConnectorDiscoveryService connectorDiscoveryService() {
        return new BpnlAndDsp08ConnectorDiscoveryServiceImpl(
                bdrsClient, httpClient, didResolver, typeManager.getMapper(),
                new CacheConfig(connectorDiscoveryCacheExpiry, clock), monitor);
    }
}
