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

package org.eclipse.tractusx.edc.transferprocess.sftp.client;

import java.util.Map;
import lombok.SneakyThrows;
import org.apache.sshd.sftp.client.SftpClient;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpDataAddress;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpLocation;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpUser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class SftpDataSourceFactoryTest {

  @Test
  void validate__valid() {
    SftpDataSourceFactory dataSourceFactory = new SftpDataSourceFactory();
    SftpUser sftpUser = SftpUser.builder().name("name").build();
    SftpLocation sftpLocation = SftpLocation.builder().host("host").port(22).path("path").build();

    SftpDataAddress sftpDataAddress =
        SftpDataAddress.builder().sftpUser(sftpUser).sftpLocation(sftpLocation).build();
    DataFlowRequest request = Mockito.mock(DataFlowRequest.class);
    Mockito.when(request.getSourceDataAddress()).thenReturn(sftpDataAddress);

    Assertions.assertTrue(dataSourceFactory.validate(request).succeeded());
  }

  @Test
  void validate__invalidDataAddressType() {
    SftpDataSourceFactory dataSourceFactory = new SftpDataSourceFactory();
    DataAddress dataAddress = DataAddress.Builder.newInstance().type("wrong").build();
    DataFlowRequest request = Mockito.mock(DataFlowRequest.class);
    Mockito.when(request.getSourceDataAddress()).thenReturn(dataAddress);

    Assertions.assertTrue(dataSourceFactory.validate(request).failed());
  }

  @Test
  void validate__invalidDataAddressParameters() {
    SftpDataSourceFactory dataSourceFactory = new SftpDataSourceFactory();
    final Map<String, String> properties =
        Map.of(
            "type", "sftp",
            "locationHost", "localhost",
            "locationPort", "notANumber",
            "locationPath", "path",
            "userName", "name",
            "userPassword", "password");

    DataAddress dataAddress = DataAddress.Builder.newInstance().properties(properties).build();
    DataFlowRequest request = Mockito.mock(DataFlowRequest.class);
    Mockito.when(request.getSourceDataAddress()).thenReturn(dataAddress);

    Assertions.assertTrue(dataSourceFactory.validate(request).failed());
  }

  @Test
  @SneakyThrows
  void createSink__successful() {
    SftpDataSourceFactory dataSourceFactory = new SftpDataSourceFactory();
    SftpUser sftpUser = SftpUser.builder().name("name").build();
    SftpLocation sftpLocation =
        SftpLocation.builder().host("127.0.0.1").port(22).path("path").build();
    SftpClientConfig sftpClientConfig =
        SftpClientConfig.builder().sftpUser(sftpUser).sftpLocation(sftpLocation).build();

    SftpDataAddress sftpDataAddress =
        SftpDataAddress.builder().sftpUser(sftpUser).sftpLocation(sftpLocation).build();
    DataFlowRequest request = Mockito.mock(DataFlowRequest.class);
    Mockito.when(request.getSourceDataAddress()).thenReturn(sftpDataAddress);

    try (MockedStatic<SftpClientWrapperImpl> staticWrapper =
        Mockito.mockStatic(SftpClientWrapperImpl.class)) {
      staticWrapper
          .when(() -> SftpClientWrapperImpl.getSftpClient(sftpClientConfig))
          .thenReturn(Mockito.mock(SftpClient.class));
      Assertions.assertNotNull(dataSourceFactory.createSource(request));

      staticWrapper.verify(
          () -> SftpClientWrapperImpl.getSftpClient(Mockito.any()), Mockito.times(1));
    }
  }

  @Test
  @SneakyThrows
  void createSink__invalidDataAddressType() {
    SftpDataSourceFactory dataSourceFactory = new SftpDataSourceFactory();
    DataAddress dataAddress = DataAddress.Builder.newInstance().type("wrong").build();
    DataFlowRequest request = Mockito.mock(DataFlowRequest.class);
    Mockito.when(request.getSourceDataAddress()).thenReturn(dataAddress);

    Assertions.assertNull(dataSourceFactory.createSource(request));
  }
}
