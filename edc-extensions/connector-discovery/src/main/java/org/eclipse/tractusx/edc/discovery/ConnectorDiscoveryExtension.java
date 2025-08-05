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

package org.eclipse.tractusx.edc.discovery;

import org.eclipse.edc.connector.controlplane.services.spi.protocol.VersionService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;
import org.eclipse.tractusx.edc.discovery.api.ConnectorDiscoveryV4AlphaController;
import org.eclipse.tractusx.edc.discovery.models.ConnectorParamsDiscoveryRequest;
import org.eclipse.tractusx.edc.discovery.service.ConnectorDiscoveryServiceImpl;
import org.eclipse.tractusx.edc.discovery.transformers.JsonObjectToConnectorDiscoveryRequest;
import org.eclipse.tractusx.edc.discovery.validators.ConnectorDiscoveryRequestValidator;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;

import static org.eclipse.tractusx.edc.discovery.ConnectorDiscoveryExtension.NAME;

@Extension(value = NAME)
public class ConnectorDiscoveryExtension implements ServiceExtension {

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
    private BdrsClient bdrsClient;
    @Inject
    private VersionService versionService;
    @Inject
    private TypeManager typeManager;
    @Inject
    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var managementTypeTransformerRegistry = transformerRegistry.forContext("management-api");

        managementTypeTransformerRegistry.register(new JsonObjectToConnectorDiscoveryRequest());
        validatorRegistry.register(ConnectorParamsDiscoveryRequest.TYPE, ConnectorDiscoveryRequestValidator.instance());

        webService.registerResource(ApiContext.MANAGEMENT, new ConnectorDiscoveryV4AlphaController(new ConnectorDiscoveryServiceImpl(bdrsClient, versionService, typeManager.getMapper()), managementTypeTransformerRegistry, validatorRegistry));
    }
}