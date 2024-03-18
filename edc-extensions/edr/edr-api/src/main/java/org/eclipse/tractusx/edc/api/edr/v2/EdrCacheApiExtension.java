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

package org.eclipse.tractusx.edc.api.edr.v2;

import jakarta.json.Json;
import org.eclipse.edc.connector.api.management.configuration.ManagementApiConfiguration;
import org.eclipse.edc.edr.spi.store.EndpointDataReferenceStore;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.tractusx.edc.api.edr.v2.dto.NegotiateEdrRequestDto;
import org.eclipse.tractusx.edc.api.edr.v2.transform.EndpointDataReferenceToDataAddressTransformer;
import org.eclipse.tractusx.edc.api.edr.v2.transform.JsonObjectFromEndpointDataReferenceEntryTransformer;
import org.eclipse.tractusx.edc.api.edr.v2.transform.JsonObjectToNegotiateEdrRequestDtoTransformer;
import org.eclipse.tractusx.edc.api.edr.v2.transform.NegotiateEdrRequestDtoToNegotiatedEdrRequestTransformer;
import org.eclipse.tractusx.edc.api.edr.v2.validation.NegotiateEdrRequestDtoValidator;
import org.eclipse.tractusx.edc.edr.spi.service.EdrService;
import org.eclipse.tractusx.edc.spi.tokenrefresh.common.TokenRefreshHandler;

import java.util.Map;

import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_NAMESPACE;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_PREFIX;

public class EdrCacheApiExtension implements ServiceExtension {

    @Inject
    private WebService webService;
    @Inject
    private ManagementApiConfiguration apiConfig;

    @Inject
    private EdrService edrService;

    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Inject
    private JsonLd jsonLdService;

    @Inject
    private JsonObjectValidatorRegistry validatorRegistry;

    @Inject
    private Monitor monitor;
    @Inject
    private EndpointDataReferenceStore edrStore;
    @Inject
    private TokenRefreshHandler tokenRefreshHandler;

    @Override
    public void initialize(ServiceExtensionContext context) {
        jsonLdService.registerNamespace(TX_PREFIX, TX_NAMESPACE);
        var mgmtApiTransformerRegistry = transformerRegistry.forContext("management-api");
        mgmtApiTransformerRegistry.register(new NegotiateEdrRequestDtoToNegotiatedEdrRequestTransformer());
        mgmtApiTransformerRegistry.register(new JsonObjectToNegotiateEdrRequestDtoTransformer());
        mgmtApiTransformerRegistry.register(new JsonObjectFromEndpointDataReferenceEntryTransformer(Json.createBuilderFactory(Map.of())));
        mgmtApiTransformerRegistry.register(new EndpointDataReferenceToDataAddressTransformer());
        validatorRegistry.register(NegotiateEdrRequestDto.EDR_REQUEST_DTO_TYPE, NegotiateEdrRequestDtoValidator.instance());
        webService.registerResource(apiConfig.getContextAlias(), new EdrCacheApiController(edrStore, mgmtApiTransformerRegistry, validatorRegistry, monitor, edrService, tokenRefreshHandler));
    }
}
