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

package org.eclipse.tractusx.edc.edr.core;

import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.tractusx.edc.edr.core.service.EdrServiceImpl;
import org.eclipse.tractusx.edc.edr.spi.service.EdrService;
import org.eclipse.tractusx.edc.edr.spi.store.EndpointDataReferenceCache;

/**
 * Registers default services for the EDR cache.
 */
@Extension(value = EdrCoreExtension.NAME)
public class EdrCoreExtension implements ServiceExtension {
    static final String NAME = "EDR Core";


    @Inject
    private Monitor monitor;

    @Inject
    private ContractNegotiationService contractNegotiationService;
    
    @Inject
    private EndpointDataReferenceCache endpointDataReferenceCache;

    @Override
    public String name() {
        return NAME;
    }


    @Provider
    public EdrService adapterTransferProcessService() {
        return new EdrServiceImpl(contractNegotiationService, endpointDataReferenceCache);
    }
}
