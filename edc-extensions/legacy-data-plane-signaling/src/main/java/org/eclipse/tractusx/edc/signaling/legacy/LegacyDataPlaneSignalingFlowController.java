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

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.DataplaneMetadata;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyArchive;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowPropertiesProvider;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.TransferTypeParser;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataAddressStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClient;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClientFactory;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowResponseMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.web.spi.configuration.context.ControlApiUrl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.CONSUMER;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;

/**
 * Implementation of {@link DataFlowController} that is compliant with the data plane signaling.
 * <p>
 * It handles all the transfer process where the transferType met the criteria defined in the format mapping of the
 * signaling spec
 * This implementation will soon be replaced by the one supporting the upcoming Data Plane Signaling spec.
 * <p>
 * Backport of the upstream {@code org.eclipse.edc.connector.controlplane.transfer.dataplane.flow.LegacyDataPlaneSignalingFlowController}
 * of EDC 0.18.2 (module {@code transfer-data-plane-signaling}), which upstream deprecated. It is unchanged except for
 * {@link #resume(TransferProcess)}, which restores the resume behaviour of EDC 0.17, see there.
 *
 * @see <a href="https://github.com/eclipse-edc/Connector/blob/main/docs/developer/data-plane-signaling/data-plane-signaling.md">Data plane signaling</a>
 * @see <a href="https://github.com/eclipse-edc/Connector/blob/main/docs/developer/data-plane-signaling/data-plane-signaling-mapping.md">Data plane signaling transfer type mapping</a>
 */
public class LegacyDataPlaneSignalingFlowController implements DataFlowController {

    private final ControlApiUrl callbackUrl;
    private final DataPlaneSelectorService selectorClient;
    private final DataPlaneClientFactory clientFactory;
    private final DataFlowPropertiesProvider propertiesProvider;
    private final String selectionStrategy;
    private final TransferTypeParser transferTypeParser;
    private final AssetIndex assetIndex;
    private final DataAddressStore dataAddressStore;
    private final PolicyArchive policyArchive;
    private final Monitor monitor;

    public LegacyDataPlaneSignalingFlowController(ControlApiUrl callbackUrl, DataPlaneSelectorService selectorClient,
                                                  DataFlowPropertiesProvider propertiesProvider, DataPlaneClientFactory clientFactory,
                                                  String selectionStrategy, TransferTypeParser transferTypeParser, AssetIndex assetIndex,
                                                  DataAddressStore dataAddressStore, PolicyArchive policyArchive, Monitor monitor) {
        this.callbackUrl = callbackUrl;
        this.selectorClient = selectorClient;
        this.propertiesProvider = propertiesProvider;
        this.clientFactory = clientFactory;
        this.selectionStrategy = selectionStrategy;
        this.transferTypeParser = transferTypeParser;
        this.assetIndex = assetIndex;
        this.dataAddressStore = dataAddressStore;
        this.policyArchive = policyArchive;
        this.monitor = monitor;
    }

    @Override
    public boolean canHandle(TransferProcess transferProcess) {
        return transferTypeParser.parse(transferProcess.getTransferType()).succeeded();
    }

    @Override
    public StatusResult<DataFlowResponse> prepare(TransferProcess transferProcess, Policy policy) {
        var dataAddress = dataAddressStore.resolve(transferProcess).orElse(f -> null);
        var selection = selectorClient.select(selectionStrategy, dataPlane ->
                dataPlane.canProvisionDestination(dataAddress));
        if (selection.failed()) {
            monitor.warning("Data Flow preparation failed, please note that this phase will become mandatory in " +
                    "the upcoming versions so please ensure that there's a data-plane able to manage the transfer-type " +
                    "%s. Error: %s".formatted(transferProcess.getTransferType(), selection.getFailureDetail()));
            return StatusResult.success(DataFlowResponse.Builder.newInstance().build());
        }

        var transferTypeParse = transferTypeParser.parse(transferProcess.getTransferType());
        if (transferTypeParse.failed()) {
            return StatusResult.failure(FATAL_ERROR, transferTypeParse.getFailureDetail());
        }

        var propertiesResult = propertiesProvider.propertiesFor(transferProcess, policy);
        if (propertiesResult.failed()) {
            return StatusResult.failure(FATAL_ERROR, propertiesResult.getFailureDetail());
        }

        var dataFlowRequest = DataFlowProvisionMessage.Builder.newInstance()
                .processId(transferProcess.getId())
                .destination(dataAddress)
                .participantId(policy.getAssignee())
                .agreementId(transferProcess.getContractId())
                .assetId(transferProcess.getAssetId())
                .transferType(transferTypeParse.getContent())
                .callbackAddress(callbackUrl != null ? callbackUrl.get() : null)
                .properties(propertiesResult.getContent())
                .build();

        var dataPlaneInstance = selection.getContent();
        return clientFactory.createClient(dataPlaneInstance)
                .prepare(dataFlowRequest)
                .map(it -> toResponse(it, dataPlaneInstance));
    }

    @Override
    public @NotNull StatusResult<DataFlowResponse> start(TransferProcess transferProcess, Policy policy) {
        var transferTypeParse = transferTypeParser.parse(transferProcess.getTransferType());
        if (transferTypeParse.failed()) {
            return StatusResult.failure(FATAL_ERROR, transferTypeParse.getFailureDetail());
        }

        var propertiesResult = propertiesProvider.propertiesFor(transferProcess, policy);
        if (propertiesResult.failed()) {
            return StatusResult.failure(FATAL_ERROR, propertiesResult.getFailureDetail());
        }

        var dataAddress = dataAddressStore.resolve(transferProcess).orElse(f -> null);
        var dataFlowRequest = DataFlowStartMessage.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .processId(transferProcess.getId())
                .sourceDataAddress(transferProcess.getContentDataAddress())
                .destinationDataAddress(dataAddress)
                .participantId(policy.getAssignee())
                .agreementId(transferProcess.getContractId())
                .assetId(transferProcess.getAssetId())
                .transferType(transferTypeParse.getContent())
                .callbackAddress(callbackUrl != null ? callbackUrl.get() : null)
                .properties(propertiesResult.getContent())
                .build();

        var selection = selectorClient.select(selectionStrategy, dataPlane -> dataPlane.canHandle(transferProcess.getContentDataAddress(), transferProcess.getTransferType()));
        if (!selection.succeeded()) {
            return StatusResult.failure(FATAL_ERROR, selection.getFailureDetail());
        }

        var dataPlaneInstance = selection.getContent();
        return clientFactory.createClient(dataPlaneInstance)
                .start(dataFlowRequest)
                .map(it -> toResponse(it, dataPlaneInstance));
    }

    @Override
    public StatusResult<Void> suspend(TransferProcess transferProcess) {
        if (transferProcess.getDataPlaneId() == null) {
            return StatusResult.success();
        }
        return Optional.ofNullable(transferProcess.getDataPlaneId())
                .map(StatusResult::success)
                .orElse(StatusResult.failure(FATAL_ERROR, "DataPlane id is null"))
                .compose(this::getClientForDataplane)
                .map(client -> client.suspend(transferProcess.getId()))
                .orElse(f -> {
                    var message = "Failed to select the data plane for suspending the transfer process %s. %s"
                            .formatted(transferProcess.getId(), f.getFailureDetail());
                    return StatusResult.failure(FATAL_ERROR, message);
                });
    }

    /**
     * Resumes the data flow of a suspended transfer process, with the behaviour of EDC 0.17.
     * <p>
     * The upstream implementation calls {@link #start(TransferProcess, Policy)} with an empty policy for both
     * participants, since EDC 0.18 invokes {@code resume} on the consumer as well. That breaks the legacy signaling:
     * <ul>
     *     <li>on the provider, the data plane receives no participant id, so the new EDR has no audience, and
     *     {@link DataFlowPropertiesProvider}s get a policy without assignee;</li>
     *     <li>on the consumer, there is no data flow to resume (e.g. for a PULL transfer), so selecting a data plane
     *     fails and the transfer process is terminated.</li>
     * </ul>
     * As in EDC 0.17, the provider restarts the data flow with the policy of the contract agreement, and the consumer
     * does not signal its data plane.
     * <p>
     * On the consumer, the data address the provider sent with the {@code TransferStartMessage} (e.g. the new EDR of a
     * PULL transfer) has been put in the {@link DataAddressStore}, but the {@code TransferProcessStarted} event that
     * follows the resumption carries the content data address of the transfer process, which still holds the data
     * address of the first start. As in EDC 0.17, the content data address is updated with the new one, so that event
     * listeners such as the EDR store receive it.
     */
    @Override
    public StatusResult<DataFlowResponse> resume(TransferProcess transferProcess) {
        if (transferProcess.getType() == CONSUMER) {
            if (transferProcess.getDataAddressAlias() != null) {
                dataAddressStore.resolve(transferProcess)
                        .onSuccess(transferProcess::setContentDataAddress)
                        .onFailure(f -> monitor.warning("Cannot resolve the data address of resumed transfer process %s: %s"
                                .formatted(transferProcess.getId(), f.getFailureDetail())));
            }
            return StatusResult.success(DataFlowResponse.Builder.newInstance().build());
        }

        var policy = policyArchive.findPolicyForContract(transferProcess.getContractId());
        if (policy == null) {
            return StatusResult.failure(FATAL_ERROR, "No policy found for contract %s of transfer process %s"
                    .formatted(transferProcess.getContractId(), transferProcess.getId()));
        }
        return start(transferProcess, policy);
    }

    @Override
    public StatusResult<Void> terminate(TransferProcess transferProcess) {
        var dataPlaneId = transferProcess.getDataPlaneId();
        if (dataPlaneId == null) {
            return StatusResult.success();
        }

        return getClientForDataplane(dataPlaneId)
                .map(client -> client.terminate(transferProcess.getId()))
                .orElse(f -> {
                    var message = "Failed to select the data plane for terminating the transfer process %s. %s"
                            .formatted(transferProcess.getId(), f.getFailureDetail());
                    return StatusResult.failure(FATAL_ERROR, message);
                });
    }

    @Override
    public StatusResult<Void> started(TransferProcess transferProcess) {
        return StatusResult.success(); // no-op for legacy protocol
    }

    @Override
    public StatusResult<Void> completed(TransferProcess transferProcess) {
        return terminate(transferProcess);
    }

    @Override
    public Set<String> transferTypesFor(Asset asset) {
        var allDataPlanes = selectorClient.getAll();
        if (allDataPlanes.failed()) {
            return emptySet();
        }

        var assetProfiles = Optional.ofNullable(asset.getDataplaneMetadata())
                .map(DataplaneMetadata::getProfiles)
                .orElse(List.of());

        var assetDataAddress = asset.getDataAddress();
        if (assetDataAddress == null) {
            return allDataPlanes.getContent().stream()
                    .filter(dataPlane -> assetProfiles.isEmpty() || !Collections.disjoint(dataPlane.getAllowedTransferTypes(), assetProfiles))
                    .flatMap(dataPlane -> dataPlane.getAllowedTransferTypes().stream())
                    .collect(toSet());
        }

        var expectedResponseChannelType = Optional.ofNullable(assetDataAddress.getResponseChannel())
                .map(DataAddress::getType)
                .orElse(null);

        return allDataPlanes.getContent().stream()
                .filter(dataPlane -> dataPlane.getAllowedSourceTypes().contains(assetDataAddress.getType()))
                .filter(dataPlane -> assetProfiles.isEmpty() || !Collections.disjoint(dataPlane.getAllowedTransferTypes(), assetProfiles))
                .flatMap(dataPlane -> dataPlane.getAllowedTransferTypes().stream())
                .filter(transferType -> isCompatibleTransferType(transferType, expectedResponseChannelType))
                .collect(toSet());
    }

    @Override
    public Set<String> transferTypesFor(String assetId) {
        return Optional.ofNullable(assetIndex.findById(assetId))
                .map(this::transferTypesFor)
                .orElseGet(Collections::emptySet);
    }

    private boolean isCompatibleTransferType(String transferType, @Nullable String expectedResponseChannelType) {
        return transferTypeParser.parse(transferType)
                .map(allowedType -> Objects.equals(allowedType.responseChannelType(), expectedResponseChannelType))
                .orElse(failure -> false);
    }

    private DataFlowResponse toResponse(DataFlowResponseMessage it, DataPlaneInstance dataPlaneInstance) {
        return DataFlowResponse.Builder.newInstance()
                .dataAddress(it.getDataAddress())
                .dataPlaneId(dataPlaneInstance.getId())
                .async(it.isProvisioning())
                .build();
    }

    private StatusResult<DataPlaneClient> getClientForDataplane(String id) {
        return selectorClient.findById(id)
                .map(clientFactory::createClient)
                .map(StatusResult::success)
                .orElse(f -> StatusResult.failure(FATAL_ERROR, "No data-plane found with id %s. %s".formatted(id, f.getFailureDetail())));
    }

}
