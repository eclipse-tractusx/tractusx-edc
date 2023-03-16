/*
 * Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.edc.transferprocess.sftp.provisioner;

import static org.eclipse.tractusx.edc.transferprocess.sftp.provisioner.NoOpSftpProvisioner.DATA_ADDRESS_TYPE;
import static org.eclipse.tractusx.edc.transferprocess.sftp.provisioner.NoOpSftpProvisioner.PROVIDER_TYPE;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import lombok.SneakyThrows;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpDataAddress;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpLocation;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpUser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SftpProviderResourceDefinitionGeneratorTest {
  private final SftpProviderResourceDefinitionGenerator generator =
      new SftpProviderResourceDefinitionGenerator();

  @Test
  void generate__successful() {
    final String name = "name";
    final String password = "password";
    final KeyPair keyPair = generateKeyPair();
    final String host = "host";
    final Integer port = 22;
    final String path = "path";

    final DataRequest dataRequest =
        DataRequest.Builder.newInstance().destinationType(DATA_ADDRESS_TYPE).build();
    final SftpUser sftpUser =
        SftpUser.builder().name(name).password(password).keyPair(keyPair).build();
    final SftpLocation sftpLocation =
        SftpLocation.builder().host(host).port(port).path(path).build();
    final DataAddress dataAddress = new SftpDataAddress(sftpUser, sftpLocation);
    final Policy policy = Policy.Builder.newInstance().build();

    final SftpProviderResourceDefinition resourceDefinition =
        (SftpProviderResourceDefinition) generator.generate(dataRequest, dataAddress, policy);

    Assertions.assertNotNull(resourceDefinition);
    final SftpDataAddress sftpDataAddress = resourceDefinition.getSftpDataAddress();

    Assertions.assertEquals(PROVIDER_TYPE, resourceDefinition.getProviderType());
    Assertions.assertEquals(host, sftpDataAddress.getSftpLocation().getHost());
    Assertions.assertEquals(port, sftpDataAddress.getSftpLocation().getPort());
    Assertions.assertEquals(path, sftpDataAddress.getSftpLocation().getPath());
    Assertions.assertEquals(name, sftpDataAddress.getSftpUser().getName());
    Assertions.assertEquals(password, sftpDataAddress.getSftpUser().getPassword());
    Assertions.assertEquals(keyPair, sftpDataAddress.getSftpUser().getKeyPair());
  }

  @Test
  void generate__wrongDataAddressType() {
    final DataRequest dataRequest =
        DataRequest.Builder.newInstance().destinationType(DATA_ADDRESS_TYPE).build();
    final DataAddress dataAddress = DataAddress.Builder.newInstance().type("wrong").build();
    final Policy policy = Policy.Builder.newInstance().build();

    final SftpProviderResourceDefinition resourceDefinition =
        (SftpProviderResourceDefinition) generator.generate(dataRequest, dataAddress, policy);

    Assertions.assertNull(resourceDefinition);
  }

  @SneakyThrows
  static KeyPair generateKeyPair() {
    final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    keyPairGenerator.initialize(2048);
    return keyPairGenerator.generateKeyPair();
  }
}
