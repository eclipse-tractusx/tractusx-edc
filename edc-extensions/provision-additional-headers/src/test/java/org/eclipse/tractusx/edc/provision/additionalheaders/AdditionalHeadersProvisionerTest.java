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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.Mockito.mock;

import java.util.UUID;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionResponse;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedDataAddressResource;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.ResourceDefinition;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;
import org.junit.jupiter.api.Test;

class AdditionalHeadersProvisionerTest {

  private final AdditionalHeadersProvisioner provisioner = new AdditionalHeadersProvisioner();

  @Test
  void canProvisionAdditionalHeadersResourceDefinition() {
    assertThat(provisioner.canProvision(mock(AdditionalHeadersResourceDefinition.class))).isTrue();
    assertThat(provisioner.canProvision(mock(ResourceDefinition.class))).isFalse();
  }

  @Test
  void cannotDeprovisionAdditionalHeadersResourceDefinition() {
    assertThat(provisioner.canDeprovision(mock(AdditionalHeadersProvisionedResource.class)))
        .isFalse();
    assertThat(provisioner.canDeprovision(mock(ProvisionedResource.class))).isFalse();
  }

  @Test
  void shouldAddContractIdAdditionalHeader() {
    var address = HttpDataAddress.Builder.newInstance().baseUrl("http://any").build();
    var resourceDefinition =
        AdditionalHeadersResourceDefinition.Builder.newInstance()
            .id(UUID.randomUUID().toString())
            .transferProcessId(UUID.randomUUID().toString())
            .contractId("contractId")
            .dataAddress(address)
            .build();

    var result = provisioner.provision(resourceDefinition, Policy.Builder.newInstance().build());

    assertThat(result)
        .succeedsWithin(5, SECONDS)
        .matches(StatusResult::succeeded)
        .extracting(StatusResult::getContent)
        .extracting(ProvisionResponse::getResource)
        .asInstanceOf(type(AdditionalHeadersProvisionedResource.class))
        .extracting(ProvisionedDataAddressResource::getDataAddress)
        .extracting(a -> HttpDataAddress.Builder.newInstance().copyFrom(a).build())
        .extracting(HttpDataAddress::getAdditionalHeaders)
        .asInstanceOf(map(String.class, String.class))
        .containsEntry("Edc-Contract-Agreement-Id", "contractId");
  }
}
