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
import static org.assertj.core.api.InstanceOfAssertFactories.type;

import java.util.UUID;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;
import org.junit.jupiter.api.Test;

class AdditionalHeadersResourceDefinitionGeneratorTest {

  private final AdditionalHeadersResourceDefinitionGenerator generator =
      new AdditionalHeadersResourceDefinitionGenerator();

  @Test
  void canGenerate_shouldReturnFalseForNotHttpDataAddresses() {
    var dataAddress = DataAddress.Builder.newInstance().type("any").build();
    var dataRequest =
        DataRequest.Builder.newInstance()
            .id(UUID.randomUUID().toString())
            .dataDestination(dataAddress)
            .build();
    var build = Policy.Builder.newInstance().build();

    var result = generator.canGenerate(dataRequest, dataAddress, build);

    assertThat(result).isFalse();
  }

  @Test
  void canGenerate_shouldReturnTrueForHttpDataAddresses() {
    var dataAddress = DataAddress.Builder.newInstance().type("HttpData").build();
    var dataRequest =
        DataRequest.Builder.newInstance()
            .id(UUID.randomUUID().toString())
            .dataDestination(dataAddress)
            .build();
    var build = Policy.Builder.newInstance().build();

    var result = generator.canGenerate(dataRequest, dataAddress, build);

    assertThat(result).isTrue();
  }

  @Test
  void shouldCreateResourceDefinitionWithDataAddress() {
    var dataAddress = HttpDataAddress.Builder.newInstance().baseUrl("http://any").build();
    var dataRequest =
        DataRequest.Builder.newInstance()
            .id(UUID.randomUUID().toString())
            .dataDestination(dataAddress)
            .build();
    var build = Policy.Builder.newInstance().build();

    var result = generator.generate(dataRequest, dataAddress, build);

    assertThat(result)
        .asInstanceOf(type(AdditionalHeadersResourceDefinition.class))
        .extracting(AdditionalHeadersResourceDefinition::getDataAddress)
        .extracting(address -> HttpDataAddress.Builder.newInstance().copyFrom(address).build())
        .extracting(HttpDataAddress::getBaseUrl)
        .isEqualTo("http://any");
  }
}
