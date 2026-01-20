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
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.jersey.providers.jsonld.JerseyJsonLdInterceptor;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;
import org.eclipse.tractusx.edc.discovery.v4alpha.api.ConnectorDiscoveryV4AlphaController;
import org.eclipse.tractusx.edc.discovery.v4alpha.service.BaseConnectorDiscoveryServiceImpl;
import org.eclipse.tractusx.edc.discovery.v4alpha.service.DefaultConnectorDiscoveryServiceImpl;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorDiscoveryRequest;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorDiscoveryService;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorParamsDiscoveryRequest;
import org.eclipse.tractusx.edc.discovery.v4alpha.transformers.JsonObjectToConnectorDiscoveryRequest;
import org.eclipse.tractusx.edc.discovery.v4alpha.transformers.JsonObjectToConnectorParamsDiscoveryRequest;
import org.eclipse.tractusx.edc.discovery.v4alpha.validators.ConnectorDiscoveryRequestValidator;
import org.eclipse.tractusx.edc.discovery.v4alpha.validators.ConnectorParamsDiscoveryRequestValidator;

import java.time.Clock;

import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;
import static org.eclipse.tractusx.edc.discovery.v4alpha.ConnectorDiscoveryExtension.NAME;

@Extension(value = NAME)
public class ConnectorDiscoveryExtension implements ServiceExtension {

    public static final String TX_EDC_CONNECTOR_DISCOVERY_CACHE_EXPIRY = "tx.edc.connector.discovery.cache.expiry";

    public static final String NAME = "Connector Discovery API Extension";

    @Override
    public String name() {
        return NAME;
    }

    @Inject
    private WebService webService;
    @Inject
    private TypeTransformerRegistry transformerRegistry;
    @Inject
    private JsonObjectValidatorRegistry validatorRegistry;
    @Inject
    private ConnectorDiscoveryService connectorDiscoveryService;
    @Inject
    private JsonLd jsonLd;
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

    @Override
    public void initialize(ServiceExtensionContext context) {
        var managementTypeTransformerRegistry = transformerRegistry.forContext("management-api");

        managementTypeTransformerRegistry.register(new JsonObjectToConnectorParamsDiscoveryRequest());
        validatorRegistry.register(ConnectorParamsDiscoveryRequest.TYPE, ConnectorParamsDiscoveryRequestValidator.instance());

        managementTypeTransformerRegistry.register(new JsonObjectToConnectorDiscoveryRequest());
        validatorRegistry.register(ConnectorDiscoveryRequest.TYPE, ConnectorDiscoveryRequestValidator.instance());

        webService.registerResource(ApiContext.MANAGEMENT, new ConnectorDiscoveryV4AlphaController(
                connectorDiscoveryService, managementTypeTransformerRegistry, validatorRegistry, monitor));
        webService.registerDynamicResource(
                ApiContext.MANAGEMENT, ConnectorDiscoveryV4AlphaController.class,
                new JerseyJsonLdInterceptor(jsonLd, typeManager, JSON_LD, "MANAGEMENT_API"));

    }

    @Provider(isDefault = true)
    public ConnectorDiscoveryService defaultConnectorDiscoveryService() {
        return new DefaultConnectorDiscoveryServiceImpl(httpClient, didResolver, typeManager.getMapper(),
                new BaseConnectorDiscoveryServiceImpl.CacheConfig(connectorDiscoveryCacheExpiry, clock), monitor);
    }
}
