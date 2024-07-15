/*
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
 */

package org.eclipse.tractusx.edc.api.edr;

import jakarta.json.Json;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.edr.spi.store.EndpointDataReferenceStore;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;
import org.eclipse.tractusx.edc.api.edr.transform.JsonObjectFromEndpointDataReferenceEntryTransformer;
import org.eclipse.tractusx.edc.api.edr.v2.EdrCacheApiV2Controller;
import org.eclipse.tractusx.edc.api.edr.v3.EdrCacheApiV3Controller;
import org.eclipse.tractusx.edc.edr.spi.service.EdrService;

import java.util.Map;

import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_NAMESPACE;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_PREFIX;

public class EdrCacheApiExtension implements ServiceExtension {

    @Inject
    private WebService webService;

    @Inject
    private EdrService edrService;

    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Inject
    private JsonLd jsonLdService;

    @Inject
    private JsonObjectValidatorRegistry validatorRegistry;

    @Inject
    private ContractNegotiationService contractNegotiationService;

    @Inject
    private Monitor monitor;
    @Inject
    private EndpointDataReferenceStore edrStore;

    @Override
    public void initialize(ServiceExtensionContext context) {
        jsonLdService.registerNamespace(TX_PREFIX, TX_NAMESPACE);
        var mgmtApiTransformerRegistry = transformerRegistry.forContext("management-api");
        mgmtApiTransformerRegistry.register(new JsonObjectFromEndpointDataReferenceEntryTransformer(Json.createBuilderFactory(Map.of())));
        webService.registerResource(ApiContext.MANAGEMENT, new EdrCacheApiV2Controller(edrStore, mgmtApiTransformerRegistry, validatorRegistry, monitor, edrService, contractNegotiationService));
        webService.registerResource(ApiContext.MANAGEMENT, new EdrCacheApiV3Controller(edrStore, mgmtApiTransformerRegistry, validatorRegistry, monitor, edrService, contractNegotiationService));
    }
}
