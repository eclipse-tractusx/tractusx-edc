/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.edc.dataplane.kafka;

import org.eclipse.edc.connector.dataplane.spi.edr.EndpointDataReferenceServiceRegistry;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionerManager;
import org.eclipse.edc.connector.dataplane.spi.provision.ResourceDefinitionGeneratorManager;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.tractusx.edc.dataplane.kafka.flow.KafkaEndpointDataReferenceService;
import org.eclipse.tractusx.edc.dataplane.kafka.provision.KafkaDeprovisioner;
import org.eclipse.tractusx.edc.dataplane.kafka.provision.KafkaProvisioner;
import org.eclipse.tractusx.edc.dataplane.kafka.provision.KafkaResourceDefinitionGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.eclipse.tractusx.edc.dataplane.kafka.dataaddress.KafkaBrokerDataAddressSchema.KAFKA_TYPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
class KafkaBrokerExtensionTest {

    private final ResourceDefinitionGeneratorManager generatorManager = mock();
    private final ProvisionerManager provisionerManager = mock();
    private final EndpointDataReferenceServiceRegistry edrRegistry = mock();
    private final Vault vault = mock();
    private final EdcHttpClient httpClient = mock();

    @BeforeEach
    void setUp(final ServiceExtensionContext context) {
        context.registerService(ResourceDefinitionGeneratorManager.class, generatorManager);
        context.registerService(ProvisionerManager.class, provisionerManager);
        context.registerService(EndpointDataReferenceServiceRegistry.class, edrRegistry);
        context.registerService(Vault.class, vault);
        context.registerService(EdcHttpClient.class, httpClient);
    }

    @Test
    void initialize_RegistersKafkaDataPlaneComponents(final KafkaBrokerExtension extension, final ServiceExtensionContext context) {
        extension.initialize(context);

        verify(generatorManager).registerProviderGenerator(any(KafkaResourceDefinitionGenerator.class));
        verify(provisionerManager).register(any(KafkaProvisioner.class));
        verify(provisionerManager).register(any(KafkaDeprovisioner.class));
        verify(edrRegistry).register(eq(KAFKA_TYPE), any(KafkaEndpointDataReferenceService.class));
    }
}
