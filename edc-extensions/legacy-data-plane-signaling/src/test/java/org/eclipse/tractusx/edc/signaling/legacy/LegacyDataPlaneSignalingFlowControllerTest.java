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

import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyArchive;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowPropertiesProvider;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.TransferTypeParser;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataAddressStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClient;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClientFactory;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowResponseMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.spi.types.domain.transfer.FlowType;
import org.eclipse.edc.spi.types.domain.transfer.TransferType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.CONSUMER;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.PROVIDER;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LegacyDataPlaneSignalingFlowControllerTest {

    private static final String CONTRACT_ID = "contract-id";
    private static final String CONSUMER_ID = "consumer-participant-id";
    private static final String TRANSFER_TYPE = "HttpData-PULL";

    private final DataPlaneSelectorService selectorService = mock();
    private final DataPlaneClientFactory clientFactory = mock();
    private final DataPlaneClient dataPlaneClient = mock();
    private final DataFlowPropertiesProvider propertiesProvider = mock();
    private final TransferTypeParser transferTypeParser = mock();
    private final DataAddressStore dataAddressStore = mock();
    private final PolicyArchive policyArchive = mock();

    private final LegacyDataPlaneSignalingFlowController controller = new LegacyDataPlaneSignalingFlowController(
            () -> null, selectorService, propertiesProvider, clientFactory, "random", transferTypeParser,
            mock(AssetIndex.class), dataAddressStore, policyArchive, mock(Monitor.class));

    @BeforeEach
    void setUp() {
        when(transferTypeParser.parse(TRANSFER_TYPE)).thenReturn(Result.success(new TransferType("HttpData", FlowType.PULL)));
        when(dataAddressStore.resolve(any())).thenReturn(StoreResult.notFound("none"));
        when(propertiesProvider.propertiesFor(any(), any())).thenReturn(StatusResult.success(Map.of("key", "value")));
        var dataPlane = DataPlaneInstance.Builder.newInstance().id("data-plane-id").url("http://data-plane").build();
        when(selectorService.select(anyString(), any())).thenReturn(ServiceResult.success(dataPlane));
        when(clientFactory.createClient(dataPlane)).thenReturn(dataPlaneClient);
    }

    @Test
    void resume_shouldRestartDataFlowWithAgreementPolicy_whenProvider() {
        var agreementPolicy = Policy.Builder.newInstance().assignee(CONSUMER_ID).build();
        when(policyArchive.findPolicyForContract(CONTRACT_ID)).thenReturn(agreementPolicy);
        var edr = DataAddress.Builder.newInstance().type("https://w3id.org/idsa/v4.1/HTTP").build();
        when(dataPlaneClient.start(any())).thenReturn(StatusResult.success(DataFlowResponseMessage.Builder.newInstance().dataAddress(edr).build()));
        var transferProcess = transferProcess(PROVIDER);

        var result = controller.resume(transferProcess);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent().getDataAddress()).isSameAs(edr);
        assertThat(result.getContent().getDataPlaneId()).isEqualTo("data-plane-id");
        var startMessage = ArgumentCaptor.forClass(DataFlowStartMessage.class);
        verify(dataPlaneClient).start(startMessage.capture());
        assertThat(startMessage.getValue().getParticipantId()).isEqualTo(CONSUMER_ID);
        assertThat(startMessage.getValue().getProperties()).containsEntry("key", "value");
        verify(propertiesProvider).propertiesFor(eq(transferProcess), eq(agreementPolicy));
    }

    @Test
    void resume_shouldFail_whenProviderAndNoAgreementPolicy() {
        when(policyArchive.findPolicyForContract(CONTRACT_ID)).thenReturn(null);

        var result = controller.resume(transferProcess(PROVIDER));

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailure().status()).isEqualTo(FATAL_ERROR);
        verifyNoInteractions(dataPlaneClient);
    }

    @Test
    void resume_shouldNotSignalDataPlane_whenConsumer() {
        var result = controller.resume(transferProcess(CONSUMER));

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent().getDataAddress()).isNull();
        verifyNoInteractions(selectorService, clientFactory, policyArchive, propertiesProvider);
    }

    @Test
    void resume_shouldUpdateContentDataAddressWithStoredOne_whenConsumer() {
        var newEdr = DataAddress.Builder.newInstance().type("https://w3id.org/idsa/v4.1/HTTP").property("authorization", "new").build();
        var transferProcess = transferProcess(CONSUMER);
        transferProcess.setDataAddressAlias("transfer-process-alias");
        when(dataAddressStore.resolve(transferProcess)).thenReturn(StoreResult.success(newEdr));

        var result = controller.resume(transferProcess);

        assertThat(result.succeeded()).isTrue();
        assertThat(transferProcess.getContentDataAddress()).isSameAs(newEdr);
        verifyNoInteractions(selectorService, clientFactory, policyArchive, propertiesProvider);
    }

    @Test
    void start_shouldUseGivenPolicy() {
        var policy = Policy.Builder.newInstance().assignee(CONSUMER_ID).build();
        when(dataPlaneClient.start(any())).thenReturn(StatusResult.success(DataFlowResponseMessage.Builder.newInstance().build()));

        var result = controller.start(transferProcess(PROVIDER), policy);

        assertThat(result.succeeded()).isTrue();
        var startMessage = ArgumentCaptor.forClass(DataFlowStartMessage.class);
        verify(dataPlaneClient).start(startMessage.capture());
        assertThat(startMessage.getValue().getParticipantId()).isEqualTo(CONSUMER_ID);
        verifyNoInteractions(policyArchive);
    }

    private TransferProcess transferProcess(TransferProcess.Type type) {
        return TransferProcess.Builder.newInstance()
                .id("transfer-process-id")
                .type(type)
                .contractId(CONTRACT_ID)
                .assetId("asset-id")
                .transferType(TRANSFER_TYPE)
                .contentDataAddress(DataAddress.Builder.newInstance().type("HttpData").build())
                .dataPlaneId(type == PROVIDER ? "data-plane-id" : null)
                .build();
    }
}
