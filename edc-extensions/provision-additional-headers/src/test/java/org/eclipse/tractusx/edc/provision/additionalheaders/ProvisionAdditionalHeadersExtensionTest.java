/*
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 * Copyright (c) 2021-2023 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.edc.provision.additionalheaders;

import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.validation.ContractValidationService;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessProtocolService;
import org.eclipse.edc.connector.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.connector.transfer.spi.flow.DataFlowManager;
import org.eclipse.edc.connector.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
@ExtendWith(EdcExtension.class)
class ProvisionAdditionalHeadersExtensionTest {

    private final DataFlowController dataFlowController = mock(DataFlowController.class);
    private final RemoteMessageDispatcherRegistry dispatcherRegistry = mock(RemoteMessageDispatcherRegistry.class);

    private final ContractNegotiationStore contractNegotiationStore = mock(ContractNegotiationStore.class);
    private final ContractValidationService contractValidationService = mock(ContractValidationService.class);

    @BeforeEach
    void setUp(EdcExtension extension) {
        extension.setConfiguration(Map.of("edc.ids.id", "urn:connector:test"));
        when(dataFlowController.canHandle(any(), any())).thenReturn(true);
        when(dataFlowController.initiateFlow(any(), any(), any())).thenReturn(StatusResult.success(DataFlowResponse.Builder.newInstance().build()));
        extension.registerServiceMock(RemoteMessageDispatcherRegistry.class, dispatcherRegistry);
        extension.registerServiceMock(ContractNegotiationStore.class, contractNegotiationStore);
        extension.registerServiceMock(ContractValidationService.class, contractValidationService);
    }

    @Test
    void shouldPutContractIdAsHeaderInDataAddress(
            TransferProcessProtocolService transferProcessProtocolService,
            AssetIndex assetIndex,
            DataFlowManager dataFlowManager) {

        var agreement = ContractAgreement.Builder.newInstance()
                .id("aContractId")
                .providerId("provider")
                .consumerId("consumer")
                .policy(Policy.Builder.newInstance().build())
                .assetId("assetId")
                .build();

        when(dispatcherRegistry.send(any(), any())).thenReturn(CompletableFuture.completedFuture(null));
        when(contractNegotiationStore.findContractAgreement(any())).thenReturn(agreement);
        when(contractValidationService.validateAgreement(any(), any())).thenReturn(Result.success(agreement));

        dataFlowManager.register(dataFlowController);
        var asset = Asset.Builder.newInstance().id("assetId").build();
        var dataAddress = DataAddress.Builder.newInstance().type("HttpData").build();
        assetIndex.create(asset, dataAddress);

        var transferMessage = TransferRequestMessage.Builder.newInstance()
                .id("id")
                .protocol("protocol")
                .assetId("assetId")
                .contractId("1:assetId:aContractId")
                .dataDestination(DataAddress.Builder.newInstance().type("HttpProxy").build())
                .callbackAddress("callbackAddress")
                .build();

        var result = transferProcessProtocolService.notifyRequested(transferMessage, ClaimToken.Builder.newInstance().build());

        assertThat(result).matches(ServiceResult::succeeded);

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> verify(dataFlowController).initiateFlow(any(), argThat(it -> "1:assetId:aContractId".equals(it.getProperty("header:Edc-Contract-Agreement-Id"))), any()));
    }
}
