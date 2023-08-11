/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.api.edr;

import org.eclipse.edc.connector.api.management.configuration.ManagementApiConfiguration;
import org.eclipse.edc.connector.api.management.configuration.transform.ManagementApiTypeTransformerRegistry;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.tractusx.edc.api.edr.transform.EndpointDataReferenceToDataAddressTransformer;
import org.eclipse.tractusx.edc.api.edr.transform.JsonObjectFromEndpointDataReferenceEntryTransformer;
import org.eclipse.tractusx.edc.api.edr.transform.JsonObjectToNegotiateEdrRequestDtoTransformer;
import org.eclipse.tractusx.edc.api.edr.transform.NegotiateEdrRequestDtoToNegotiatedEdrRequestTransformer;
import org.eclipse.tractusx.edc.api.edr.validation.NegotiateEdrRequestDtoValidator;
import org.eclipse.tractusx.edc.edr.spi.service.EdrService;

import static org.eclipse.tractusx.edc.api.edr.dto.NegotiateEdrRequestDto.EDR_REQUEST_DTO_TYPE;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_NAMESPACE;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_PREFIX;

public class EdrApiExtension implements ServiceExtension {

    @Inject
    private WebService webService;
    @Inject
    private ManagementApiConfiguration apiConfig;

    @Inject
    private EdrService edrService;

    @Inject
    private ManagementApiTypeTransformerRegistry transformerRegistry;

    @Inject
    private JsonLd jsonLdService;

    @Inject
    private JsonObjectValidatorRegistry validatorRegistry;

    @Inject
    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        jsonLdService.registerNamespace(TX_PREFIX, TX_NAMESPACE);
        transformerRegistry.register(new NegotiateEdrRequestDtoToNegotiatedEdrRequestTransformer());
        transformerRegistry.register(new JsonObjectToNegotiateEdrRequestDtoTransformer());
        transformerRegistry.register(new JsonObjectFromEndpointDataReferenceEntryTransformer());
        transformerRegistry.register(new EndpointDataReferenceToDataAddressTransformer());
        validatorRegistry.register(EDR_REQUEST_DTO_TYPE, NegotiateEdrRequestDtoValidator.instance());
        webService.registerResource(apiConfig.getContextAlias(), new EdrController(edrService, transformerRegistry, validatorRegistry, monitor));
    }
}
