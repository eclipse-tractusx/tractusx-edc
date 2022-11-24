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
import lombok.SneakyThrows;
import org.apache.sshd.sftp.client.SftpClient;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpLocation;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpUser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class SshdSftpClientWrapperTest {
  private final SshdSftpClientWrapper sftpClientWrapper = Mockito.spy(new SshdSftpClientWrapper());

  @Test
  @SneakyThrows
  void uploadFile() {
    SftpClient sftpClientMock = Mockito.mock(SftpClient.class);
    SftpUser userMock = Mockito.mock(SftpUser.class);
    SftpLocation locationMock = Mockito.mock(SftpLocation.class);
    byte[] content = new byte[3];
    content[1] = 1;
    content[2] = 2;
    InputStream inputStream = new ByteArrayInputStream(content);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    Mockito.doReturn(sftpClientMock).when(sftpClientWrapper).getSftpClient(userMock, locationMock);
    Mockito.when(
            sftpClientMock.write(
                Mockito.any(), Mockito.anyInt(), Mockito.any(SftpClient.OpenMode.class)))
        .thenReturn(outputStream);
    sftpClientWrapper.uploadFile(userMock, locationMock, inputStream);

    Assertions.assertArrayEquals(content, outputStream.toByteArray());
  }

  @Test
  @SneakyThrows
  void downloadFile() {
    SftpClient sftpClientMock = Mockito.mock(SftpClient.class);
    SftpUser userMock = Mockito.mock(SftpUser.class);
    SftpLocation locationMock = Mockito.mock(SftpLocation.class);
    byte[] content = new byte[3];
    content[1] = 1;
    content[2] = 2;
    InputStream inputStream = new ByteArrayInputStream(content);

    Mockito.doReturn(sftpClientMock).when(sftpClientWrapper).getSftpClient(userMock, locationMock);
    Mockito.when(sftpClientMock.read(Mockito.anyString(), Mockito.anyInt()))
        .thenReturn(inputStream);

    InputStream resultStream = sftpClientWrapper.downloadFile(userMock, locationMock);

    Assertions.assertArrayEquals(content, resultStream.readAllBytes());
  }
}
