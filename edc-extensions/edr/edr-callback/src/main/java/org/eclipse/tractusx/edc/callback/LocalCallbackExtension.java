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

package org.eclipse.tractusx.edc.callback;

import org.eclipse.edc.connector.spi.callback.CallbackProtocolResolverRegistry;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.tractusx.edc.edr.spi.store.EndpointDataReferenceCache;
import org.eclipse.tractusx.edc.spi.callback.InProcessCallbackRegistry;

import static org.eclipse.tractusx.edc.callback.InProcessCallbackMessageDispatcher.CALLBACK_EVENT_LOCAL;

@Extension(LocalCallbackExtension.NAME)
public class LocalCallbackExtension implements ServiceExtension {
    public static final String NAME = "Local callbacks extension";

    public static final String LOCAL = "local";
    @Inject
    private RemoteMessageDispatcherRegistry registry;

    @Inject
    private CallbackProtocolResolverRegistry resolverRegistry;

    @Inject
    private TransferProcessService transferProcessService;

    @Inject
    private TransferProcessStore transferProcessStore;

    @Inject
    private EndpointDataReferenceCache edrCache;

    @Inject
    private InProcessCallbackRegistry callbackRegistry;

    @Inject
    private Monitor monitor;

    @Inject
    private TransactionContext transactionContext;

    @Inject
    private EndpointDataReferenceCache endpointDataReferenceCache;

    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {

        callbackRegistry.registerHandler(new ContractNegotiationCallback(transferProcessService, monitor));
        callbackRegistry.registerHandler(new TransferProcessLocalCallback(edrCache, transferProcessStore, transformerRegistry, transactionContext, monitor));

        resolverRegistry.registerResolver(this::resolveProtocol);
        registry.register(new InProcessCallbackMessageDispatcher(callbackRegistry));

    }

    private String resolveProtocol(String scheme) {

        if (scheme.equalsIgnoreCase(LOCAL)) {
            return CALLBACK_EVENT_LOCAL;
        }
        return null;
    }
}
