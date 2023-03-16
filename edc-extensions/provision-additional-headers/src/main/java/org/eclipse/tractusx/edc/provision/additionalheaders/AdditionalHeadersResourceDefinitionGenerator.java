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

import java.util.UUID;
import org.eclipse.edc.connector.transfer.spi.provision.ProviderResourceDefinitionGenerator;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.ResourceDefinition;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.jetbrains.annotations.Nullable;

class AdditionalHeadersResourceDefinitionGenerator implements ProviderResourceDefinitionGenerator {

  @Override
  public boolean canGenerate(DataRequest dataRequest, DataAddress dataAddress, Policy policy) {
    return "HttpData".equals(dataAddress.getType());
  }

  @Override
  public @Nullable ResourceDefinition generate(
      DataRequest dataRequest, DataAddress dataAddress, Policy policy) {
    return AdditionalHeadersResourceDefinition.Builder.newInstance()
        .id(UUID.randomUUID().toString())
        .dataAddress(dataAddress)
        .contractId(dataRequest.getContractId())
        .build();
  }
}
