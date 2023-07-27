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
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.tractusx.edc.api.edr.transform.JsonObjectFromEndpointDataReferenceEntryTransformer;
import org.eclipse.tractusx.edc.api.edr.transform.JsonObjectToNegotiateEdrRequestDtoTransformer;
import org.eclipse.tractusx.edc.api.edr.transform.NegotiateEdrRequestDtoToNegotiatedEdrRequestTransformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
public class EdrApiExtensionTest {

    private final ManagementApiTypeTransformerRegistry transformerRegistry = mock();
    private final WebService webService = mock(WebService.class);
    private final ManagementApiConfiguration configuration = mock(ManagementApiConfiguration.class);
    private EdrApiExtension extension;

    @BeforeEach
    void setUp(ObjectFactory factory, ServiceExtensionContext context) {
        context.registerService(WebService.class, webService);
        context.registerService(ManagementApiTypeTransformerRegistry.class, transformerRegistry);
        context.registerService(ManagementApiConfiguration.class, configuration);
        extension = factory.constructInstance(EdrApiExtension.class);
    }

    @Test
    void initialize_ShouldConfigureTheController(ServiceExtensionContext context) {
        var alias = "context";

        when(configuration.getContextAlias()).thenReturn(alias);
        extension.initialize(context);

        verify(webService).registerResource(eq(alias), isA(EdrController.class));
        verify(transformerRegistry).register(isA(NegotiateEdrRequestDtoToNegotiatedEdrRequestTransformer.class));
        verify(transformerRegistry).register(isA(JsonObjectToNegotiateEdrRequestDtoTransformer.class));
        verify(transformerRegistry).register(isA(JsonObjectFromEndpointDataReferenceEntryTransformer.class));

    }
}
