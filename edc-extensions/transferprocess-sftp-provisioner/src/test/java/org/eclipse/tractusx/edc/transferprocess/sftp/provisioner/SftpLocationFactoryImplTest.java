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

import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpLocation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SftpLocationFactoryImplTest {
  private final SftpLocationFactoryImpl sftpLocationFactoryImpl = new SftpLocationFactoryImpl();

  @Test
  void generateSftpLocation() {
    final String host = "host";
    final Integer port = 22;
    final String path = "path";

    final SftpLocation location = sftpLocationFactoryImpl.createSftpLocation(host, port, path);

    Assertions.assertEquals(host, location.getHost());
    Assertions.assertEquals(port, location.getPort());
    Assertions.assertEquals(path, location.getPath());
  }
}
