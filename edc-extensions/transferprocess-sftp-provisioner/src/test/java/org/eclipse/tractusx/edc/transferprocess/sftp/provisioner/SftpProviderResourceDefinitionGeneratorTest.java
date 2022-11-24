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

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpDataAddress;
import org.junit.jupiter.api.Test;

public class SftpProviderResourceDefinitionGeneratorTest {
  private final SftpProviderResourceDefinitionGenerator generator =
      new SftpProviderResourceDefinitionGenerator();

  @Test
  void generate() {
    String name = "name";
    String password = "password";
    byte[] privateKey = new byte[1];
    String host = "host";
    Integer port = 22;
    String path = "path";

    DataRequest dataRequest =
        DataRequest.Builder.newInstance().destinationType(DATA_ADDRESS_TYPE).build();
    DataAddress dataAddress = new SftpDataAddress(name, password, privateKey, host, port, path);
    Policy policy = Policy.Builder.newInstance().build();

    generator.generate(dataRequest, dataAddress, policy);
  }
}
