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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

import org.eclipse.edc.connector.transfer.spi.TransferProcessManager;
import org.eclipse.edc.connector.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.connector.transfer.spi.flow.DataFlowManager;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(EdcExtension.class)
class ProvisionAdditionalHeadersExtensionTest {

  private final DataFlowController dataFlowController = mock(DataFlowController.class);

  @BeforeEach
  void setUp() {
    when(dataFlowController.canHandle(any(), any())).thenReturn(true);
    when(dataFlowController.initiateFlow(any(), any(), any())).thenReturn(StatusResult.success());
  }

  @Test
  void shouldPutContractIdAsHeaderInDataAddress(
      TransferProcessManager transferProcessManager,
      AssetIndex assetIndex,
      DataFlowManager dataFlowManager) {
    dataFlowManager.register(dataFlowController);
    var asset = Asset.Builder.newInstance().id("assetId").build();
    var dataAddress = DataAddress.Builder.newInstance().type("HttpData").build();
    assetIndex.accept(asset, dataAddress);

    var dataRequest =
        DataRequest.Builder.newInstance()
            .contractId("aContractId")
            .assetId("assetId")
            .destinationType("HttpProxy")
            .build();

    var result = transferProcessManager.initiateProviderRequest(dataRequest);

    assertThat(result).matches(StatusResult::succeeded);

    await()
        .untilAsserted(
            () -> {
              verify(dataFlowController)
                  .initiateFlow(
                      any(),
                      argThat(
                          it ->
                              "aContractId"
                                  .equals(it.getProperty("header:Edc-Contract-Agreement-Id"))),
                      any());
            });
  }
}
