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

package org.eclipse.tractusx.edc.api.cp.adapter;

import org.eclipse.edc.connector.api.management.configuration.ManagementApiConfiguration;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.tractusx.edc.api.cp.adapter.transform.JsonObjectToNegotiateEdrRequestDtoTransformer;
import org.eclipse.tractusx.edc.api.cp.adapter.transform.NegotiateEdrRequestDtoToNegotiatedEdrRequestTransformer;
import org.eclipse.tractusx.edc.spi.cp.adapter.service.AdapterTransferProcessService;

public class AdapterApiExtension implements ServiceExtension {

    @Inject
    private WebService webService;
    @Inject
    private ManagementApiConfiguration apiConfig;

    @Inject
    private AdapterTransferProcessService adapterTransferProcessService;

    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Inject
    private JsonLd jsonLdService;

    @Override
    public void initialize(ServiceExtensionContext context) {
        transformerRegistry.register(new NegotiateEdrRequestDtoToNegotiatedEdrRequestTransformer());
        transformerRegistry.register(new JsonObjectToNegotiateEdrRequestDtoTransformer());
        webService.registerResource(apiConfig.getContextAlias(), new AdapterEdrController(adapterTransferProcessService, jsonLdService, transformerRegistry));
    }
}
