/********************************************************************************
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
 ********************************************************************************/

package org.eclipse.edc.connector.dataplane.framework;

import org.eclipse.edc.connector.dataplane.framework.registry.TransferServiceRegistryImpl;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.connector.dataplane.spi.registry.TransferServiceRegistry;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
class DataPlaneFrameworkExtensionTest {

    private final PipelineService pipelineService = mock();

    @BeforeEach
    public void setUp(ServiceExtensionContext context) {
        context.registerService(PipelineService.class, pipelineService);
        context.registerService(ExecutorInstrumentation.class, ExecutorInstrumentation.noop());
    }

    @Test
    void initialize_registers_transferService(ServiceExtensionContext context, DataPlaneFrameworkExtension extension) {
        extension.initialize(context);

        assertThat(context.getService(TransferServiceRegistry.class)).isInstanceOf(TransferServiceRegistryImpl.class);
    }

    @Test
    void shouldClosePipelineService_whenShutdown(DataPlaneFrameworkExtension extension) {
        extension.shutdown();

        verify(pipelineService).closeAll();
    }
}
