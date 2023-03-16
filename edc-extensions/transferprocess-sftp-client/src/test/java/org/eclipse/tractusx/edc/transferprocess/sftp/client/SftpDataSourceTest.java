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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.apache.sshd.sftp.client.SftpClient;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpLocation;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpUser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SftpDataSourceTest {
  @Test
  @SneakyThrows
  void openPartStream() {
    final SftpUser userMock = Mockito.mock(SftpUser.class);
    final SftpLocation locationMock = Mockito.mock(SftpLocation.class);
    final SftpClientConfig sftpClientConfig =
        SftpClientConfig.builder().sftpUser(userMock).sftpLocation(locationMock).build();
    final SftpClient sftpClientMock = Mockito.mock(SftpClient.class);
    final SftpClientWrapperImpl sftpClientWrapper =
        Mockito.spy(new SftpClientWrapperImpl(sftpClientConfig, sftpClientMock));
    SftpDataSource sftpDataSource = Mockito.spy(new SftpDataSource(sftpClientWrapper));
    byte[] expected = new byte[] {0, 1, 2, 3};
    ByteArrayInputStream outputStream = new ByteArrayInputStream(expected);

    Mockito.when(locationMock.getPath()).thenReturn("path");
    Mockito.when(
            sftpClientMock.read(Mockito.anyString(), Mockito.anyInt(), Mockito.anyCollection()))
        .thenReturn(outputStream);

    Stream<DataSource.Part> partStream = sftpDataSource.openPartStream();
    DataSource.Part part = partStream.collect(Collectors.toList()).get(0);

    Assertions.assertArrayEquals(expected, part.openStream().readAllBytes());
    Mockito.verify(sftpClientMock, Mockito.times(1))
        .read("path", 4096, List.of(SftpClient.OpenMode.Read));
  }
}
