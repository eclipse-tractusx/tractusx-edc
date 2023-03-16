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
import java.util.concurrent.CompletableFuture;
import org.eclipse.edc.connector.transfer.spi.provision.Provisioner;
import org.eclipse.edc.connector.transfer.spi.types.DeprovisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionResponse;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.ResourceDefinition;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;

public class AdditionalHeadersProvisioner
    implements Provisioner<
        AdditionalHeadersResourceDefinition, AdditionalHeadersProvisionedResource> {

  @Override
  public boolean canProvision(ResourceDefinition resourceDefinition) {
    return resourceDefinition instanceof AdditionalHeadersResourceDefinition;
  }

  @Override
  public boolean canDeprovision(ProvisionedResource provisionedResource) {
    return false; // nothing to deprovision
  }

  @Override
  public CompletableFuture<StatusResult<ProvisionResponse>> provision(
      AdditionalHeadersResourceDefinition resourceDefinition, Policy policy) {

    var address =
        HttpDataAddress.Builder.newInstance()
            .copyFrom(resourceDefinition.getDataAddress())
            .addAdditionalHeader("Edc-Contract-Agreement-Id", resourceDefinition.getContractId())
            .build();

    var provisioned =
        AdditionalHeadersProvisionedResource.Builder.newInstance()
            .id(UUID.randomUUID().toString())
            .resourceDefinitionId(resourceDefinition.getId())
            .transferProcessId(resourceDefinition.getTransferProcessId())
            .dataAddress(address)
            .resourceName(UUID.randomUUID().toString())
            .hasToken(false)
            .build();

    var response = ProvisionResponse.Builder.newInstance().resource(provisioned).build();
    var result = StatusResult.success(response);
    return CompletableFuture.completedFuture(result);
  }

  @Override
  public CompletableFuture<StatusResult<DeprovisionedResource>> deprovision(
      AdditionalHeadersProvisionedResource additionalHeadersProvisionedResource, Policy policy) {
    return null; // nothing to deprovision
  }
}
