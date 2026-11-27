/********************************************************************************
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
 * Assisted-By: Claude Code (claude-opus-5-5)
 ********************************************************************************/

package org.eclipse.tractusx.edc.signaling.legacy;

import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataPlaneProtocolInUse;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.tractusx.edc.signaling.legacy.LegacyDataPlaneSignalingExtension.UPSTREAM_EXTENSION_CLASS;

@ExtendWith(DependencyInjectionExtension.class)
class LegacyDataPlaneSignalingExtensionTest {

    private final DataPlaneProtocolInUse dataPlaneProtocolInUse = new DataPlaneProtocolInUse();

    @BeforeEach
    void setup(ServiceExtensionContext context) {
        context.registerService(DataPlaneProtocolInUse.class, dataPlaneProtocolInUse);
    }

    @Test
    void initialize_shouldMarkLegacyProtocolInUse(ServiceExtensionContext context, LegacyDataPlaneSignalingExtension extension) {
        extension.initialize(context);

        assertThat(dataPlaneProtocolInUse.isLegacy()).isTrue();
    }

    @Test
    void shouldProvideLegacyDataFlowController(LegacyDataPlaneSignalingExtension extension) {
        assertThat(extension.dataFlowController()).isInstanceOf(LegacyDataPlaneSignalingFlowController.class);
    }

    @Test
    void upstreamModule_shouldNotBeOnClasspath() {
        assertThatThrownBy(() -> Class.forName(UPSTREAM_EXTENSION_CLASS)).isInstanceOf(ClassNotFoundException.class);
    }
}
