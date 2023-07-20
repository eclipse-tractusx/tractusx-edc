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
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.tractusx.edc.edr.core.manager.EdrManagerImpl;
import org.eclipse.tractusx.edc.edr.spi.EdrManager;
import org.eclipse.tractusx.edc.edr.spi.store.EndpointDataReferenceCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(DependencyInjectionExtension.class)
public class EdrCoreExtensionTest {

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(ContractNegotiationService.class, mock(ContractNegotiationService.class));
        context.registerService(EndpointDataReferenceCache.class, mock(EndpointDataReferenceCache.class));
    }

    @Test
    void shouldInitializeTheExtension(ServiceExtensionContext context, EdrCoreExtension extension) {
        extension.initialize(context);

        var service = context.getService(EdrManager.class);
        assertThat(service).isInstanceOf(EdrManagerImpl.class);

    }
}
