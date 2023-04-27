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

package org.eclipse.tractusx.edc.cp.adapter.callback;

import org.eclipse.edc.connector.spi.callback.CallbackProtocolResolverRegistry;
import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.tractusx.edc.spi.cp.adapter.service.AdapterTransferProcessService;

import static org.eclipse.tractusx.edc.cp.adapter.callback.InProcessCallbackMessageDispatcher.CALLBACK_EVENT_LOCAL;

@Provides(AdapterTransferProcessService.class)
public class LocalCallbackExtension implements ServiceExtension {

    @Inject
    private RemoteMessageDispatcherRegistry registry;

    @Inject
    private CallbackProtocolResolverRegistry resolverRegistry;

    @Inject
    private TransferProcessService transferProcessService;

    @Inject
    private ContractNegotiationService contractNegotiationService;

    @Inject
    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var callbackRegistry = new InProcessCallbackRegistryImpl();

        callbackRegistry.registerHandler(new ContractNegotiationCallback(transferProcessService, monitor));
        resolverRegistry.registerResolver(this::resolveProtocol);
        registry.register(new InProcessCallbackMessageDispatcher(callbackRegistry));

        context.registerService(AdapterTransferProcessService.class, new AdapterTransferProcessServiceImpl(contractNegotiationService));
    }

    private String resolveProtocol(String scheme) {

        if (scheme.equalsIgnoreCase("local")) {
            return CALLBACK_EVENT_LOCAL;
        }
        return null;
    }
}
