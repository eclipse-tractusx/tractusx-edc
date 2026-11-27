/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyArchive;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowPropertiesProvider;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.TransferTypeParser;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataAddressStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataPlaneProtocolInUse;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClientFactory;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.configuration.context.ControlApiUrl;

import java.util.Map;

import static org.eclipse.tractusx.edc.signaling.legacy.LegacyDataPlaneSignalingExtension.NAME;

/**
 * Provides the {@link DataFlowController} for the legacy data plane signaling, which the Tractus-X data plane speaks.
 * <p>
 * Backport of the upstream {@code TransferDataPlaneSignalingExtension} of EDC 0.18.2 (module
 * {@code transfer-data-plane-signaling}), see {@link LegacyDataPlaneSignalingFlowController}. It replaces the upstream
 * module, which must not be on the classpath as well, since both provide a {@link DataFlowController}.
 */
@Extension(NAME)
public class LegacyDataPlaneSignalingExtension implements ServiceExtension {

    public static final String NAME = "Tractus-X Legacy Data Plane Signaling";

    static final String UPSTREAM_EXTENSION_CLASS = "org.eclipse.edc.connector.controlplane.transfer.dataplane.TransferDataPlaneSignalingExtension";

    private static final String DEFAULT_DATAPLANE_SELECTOR_STRATEGY = "random";

    @Setting(description = "Defines strategy for Data Plane instance selection in case Data Plane is not embedded in current runtime",
            defaultValue = DEFAULT_DATAPLANE_SELECTOR_STRATEGY, key = "edc.dataplane.client.selector.strategy")
    private String selectionStrategy;

    @Inject(required = false)
    private ControlApiUrl callbackUrl;
    @Inject
    private DataPlaneSelectorService selectorService;
    @Inject
    private DataPlaneClientFactory clientFactory;
    @Inject(required = false)
    private DataFlowPropertiesProvider propertiesProvider;
    @Inject
    private TransferTypeParser transferTypeParser;
    @Inject
    private AssetIndex assetIndex;
    @Inject
    private DataAddressStore dataAddressStore;
    @Inject
    private PolicyArchive policyArchive;
    @Inject
    private Monitor monitor;
    @Inject
    private DataPlaneProtocolInUse dataPlaneProtocolInUse;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        if (isOnClasspath(UPSTREAM_EXTENSION_CLASS)) {
            throw new EdcException("The upstream module 'org.eclipse.edc:transfer-data-plane-signaling' must not be used " +
                    "together with '%s': both provide a DataFlowController. Exclude the upstream module from the runtime."
                            .formatted(NAME));
        }
        dataPlaneProtocolInUse.setLegacy(true);
    }

    @Provider
    public DataFlowController dataFlowController() {
        return new LegacyDataPlaneSignalingFlowController(callbackUrl, selectorService, getPropertiesProvider(),
                clientFactory, selectionStrategy, transferTypeParser, assetIndex, dataAddressStore, policyArchive,
                monitor.withPrefix(LegacyDataPlaneSignalingFlowController.class.getSimpleName()));
    }

    private DataFlowPropertiesProvider getPropertiesProvider() {
        return propertiesProvider == null ? (tp, p) -> StatusResult.success(Map.of()) : propertiesProvider;
    }

    private boolean isOnClasspath(String className) {
        try {
            Class.forName(className, false, getClass().getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
