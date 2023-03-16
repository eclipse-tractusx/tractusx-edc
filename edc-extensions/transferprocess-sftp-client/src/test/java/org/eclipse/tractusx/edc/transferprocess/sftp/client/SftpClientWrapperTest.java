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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import lombok.SneakyThrows;
import org.apache.sshd.sftp.client.SftpClient;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpLocation;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpUser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SftpClientWrapperTest {
  @Test
  @SneakyThrows
  void uploadFile() {
    SftpUser userMock = Mockito.mock(SftpUser.class);
    SftpLocation locationMock = Mockito.mock(SftpLocation.class);
    SftpClientConfig sftpClientConfig =
        SftpClientConfig.builder().sftpUser(userMock).sftpLocation(locationMock).build();
    SftpClient sftpClientMock = Mockito.mock(SftpClient.class);
    SftpClientWrapperImpl sftpClientWrapper =
        Mockito.spy(new SftpClientWrapperImpl(sftpClientConfig, sftpClientMock));
    byte[] content = new byte[] {0, 1, 2};
    InputStream inputStream = new ByteArrayInputStream(content);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    Mockito.when(locationMock.getPath()).thenReturn("path");
    Mockito.when(
            sftpClientMock.write(Mockito.anyString(), Mockito.anyInt(), Mockito.anyCollection()))
        .thenReturn(outputStream);
    sftpClientWrapper.uploadFile(inputStream);

    Assertions.assertArrayEquals(content, outputStream.toByteArray());
    Mockito.verify(sftpClientMock, Mockito.times(1))
        .write("path", 4096, List.of(SftpClient.OpenMode.Create, SftpClient.OpenMode.Write));
  }

  @Test
  @SneakyThrows
  void downloadFile() {
    SftpUser userMock = Mockito.mock(SftpUser.class);
    SftpLocation locationMock = Mockito.mock(SftpLocation.class);
    SftpClientConfig sftpClientConfig =
        SftpClientConfig.builder().sftpUser(userMock).sftpLocation(locationMock).build();
    SftpClient sftpClientMock = Mockito.mock(SftpClient.class);
    SftpClientWrapperImpl sftpClientWrapper =
        Mockito.spy(new SftpClientWrapperImpl(sftpClientConfig, sftpClientMock));
    byte[] content = new byte[] {0, 1, 2};
    InputStream inputStream = new ByteArrayInputStream(content);

    Mockito.when(locationMock.getPath()).thenReturn("path");
    Mockito.when(
            sftpClientMock.read(Mockito.anyString(), Mockito.anyInt(), Mockito.anyCollection()))
        .thenReturn(inputStream);

    try (InputStream resultStream = sftpClientWrapper.downloadFile()) {
      Assertions.assertArrayEquals(content, resultStream.readAllBytes());
      Mockito.verify(sftpClientMock, Mockito.times(1))
          .read("path", 4096, List.of(SftpClient.OpenMode.Read));
    }
  }
}
