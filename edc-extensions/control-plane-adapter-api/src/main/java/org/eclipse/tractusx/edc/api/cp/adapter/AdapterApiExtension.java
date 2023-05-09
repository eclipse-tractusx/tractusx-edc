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
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.tractusx.edc.api.cp.adapter.transform.TransferOpenRequestDtoToTransferOpenRequestTransformer;
import org.eclipse.tractusx.edc.spi.cp.adapter.service.AdapterTransferProcessService;

import java.time.Clock;

public class AdapterApiExtension implements ServiceExtension {

    @Inject
    private WebService webService;
    @Inject
    private ManagementApiConfiguration apiConfig;

    @Inject
    private AdapterTransferProcessService adapterTransferProcessService;

    @Inject
    private Clock clock;

    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var idsId = context.getSetting("edc.ids.id", null);

        transformerRegistry.register(new TransferOpenRequestDtoToTransferOpenRequestTransformer(clock, idsId));
        webService.registerResource(apiConfig.getContextAlias(), new AdapterController(adapterTransferProcessService, transformerRegistry));
    }
}
