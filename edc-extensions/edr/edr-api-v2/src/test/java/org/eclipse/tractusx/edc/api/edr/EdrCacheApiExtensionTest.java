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

import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.tractusx.edc.api.edr.transform.JsonObjectFromEndpointDataReferenceEntryTransformer;
import org.eclipse.tractusx.edc.api.edr.v2.EdrCacheApiV2Controller;
import org.eclipse.tractusx.edc.api.edr.v3.EdrCacheApiV3Controller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class EdrCacheApiExtensionTest {

    private final JsonObjectValidatorRegistry validatorRegistry = mock();
    private final WebService webService = mock();

    private final TypeTransformerRegistry transformerRegistry = mock();


    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(JsonObjectValidatorRegistry.class, validatorRegistry);
        context.registerService(WebService.class, webService);
        context.registerService(TypeTransformerRegistry.class, transformerRegistry);
        when(transformerRegistry.forContext("management-api")).thenReturn(transformerRegistry);
    }

    @Test
    void initialize_shouldRegisterControllers(EdrCacheApiExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        verify(webService).registerResource(any(), isA(EdrCacheApiV2Controller.class));
        verify(webService).registerResource(any(), isA(EdrCacheApiV3Controller.class));
    }

    @Test
    void initialize_shouldRegisterTransformers(EdrCacheApiExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);
        verify(transformerRegistry).register(isA(JsonObjectFromEndpointDataReferenceEntryTransformer.class));
    }
}
