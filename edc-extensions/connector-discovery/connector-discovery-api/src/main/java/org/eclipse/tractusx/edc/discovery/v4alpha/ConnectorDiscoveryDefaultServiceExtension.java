/*
 * Copyright (c) 2025 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.tractusx.edc.discovery.v4alpha.service.ConnectorDiscoveryServiceImpl;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorDiscoveryService;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;

import static org.eclipse.tractusx.edc.discovery.v4alpha.ConnectorDiscoveryExtension.NAME;

@Extension(value = NAME)
public class ConnectorDiscoveryDefaultServiceExtension implements ServiceExtension {

    public static final String NAME = "Default Connector Discovery Service Extension";

    @Override
    public String name() {
        return NAME;
    }

    @Inject
    private BdrsClient bdrsClient;
    @Inject
    private EdcHttpClient httpClient;
    @Inject
    private TypeManager typeManager;

    @Provider(isDefault = true)
    public ConnectorDiscoveryService defaultConnectorDiscoveryService() {
        return new ConnectorDiscoveryServiceImpl(bdrsClient, httpClient, typeManager.getMapper());
    }
}