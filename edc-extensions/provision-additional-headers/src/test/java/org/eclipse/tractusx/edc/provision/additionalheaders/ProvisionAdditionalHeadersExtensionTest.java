/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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
 ********************************************************************************/

package org.eclipse.tractusx.edc.provision.additionalheaders;

import org.eclipse.edc.connector.controlplane.transfer.spi.provision.ProvisionManager;
import org.eclipse.edc.connector.controlplane.transfer.spi.provision.ResourceManifestGenerator;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
class ProvisionAdditionalHeadersExtensionTest {

    private final ResourceManifestGenerator resourceManifestGenerator = mock();
    private final ProvisionManager provisionManager = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(ResourceManifestGenerator.class, resourceManifestGenerator);
        context.registerService(ProvisionManager.class, provisionManager);
    }

    @Test
    void initializeShouldRegisterProvisioner(ProvisionAdditionalHeadersExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        verify(resourceManifestGenerator).registerGenerator(isA(AdditionalHeadersResourceDefinitionGenerator.class));
        verify(provisionManager).register(isA(AdditionalHeadersProvisioner.class));
    }
}
