/********************************************************************************
 * Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.tractusx.edc.transferprocess.sftp.client;

import org.apache.sshd.sftp.client.SftpClient;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpLocation;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpUser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

class SftpDataSinkTest {
    @Test
    void transferParts() throws IOException {
        var userMock = Mockito.mock(SftpUser.class);
        var locationMock = Mockito.mock(SftpLocation.class);
        var sftpClientConfig =
                SftpClientConfig.Builder.newInstance()
                        .sftpUser(userMock)
                        .sftpLocation(locationMock)
                        .writeOpenModes(List.of(SftpClient.OpenMode.Create, SftpClient.OpenMode.Append))
                        .build();
        var sftpClientMock = Mockito.mock(SftpClient.class);
        var sftpClientWrapper =
                Mockito.spy(new SftpClientWrapperImpl(sftpClientConfig, sftpClientMock));
        var sftpDataSink = Mockito.spy(new SftpDataSink(sftpClientWrapper));
        var outputStream = new ByteArrayOutputStream();

        Mockito.when(sftpClientMock.write(Mockito.any(), Mockito.anyInt(), Mockito.anyCollection()))
                .thenReturn(outputStream);
        Mockito.when(locationMock.getPath()).thenReturn("path");

        List<DataSource.Part> parts =
                Arrays.asList(new SftpTestPart(new byte[]{0, 1}), new SftpTestPart(new byte[]{2, 3}));
        byte[] expected = {0, 1, 2, 3};

        sftpDataSink.transferParts(parts);

        Assertions.assertArrayEquals(expected, outputStream.toByteArray());
        Mockito.verify(sftpClientMock, Mockito.times(2))
                .write("path", 4096, List.of(SftpClient.OpenMode.Create, SftpClient.OpenMode.Append));
    }

    private static class SftpTestPart implements DataSource.Part {

        final byte[] content;

        private SftpTestPart(byte[] content) {
            this.content = content;
        }

        @Override
        public String name() {
            return null;
        }

        @Override
        public InputStream openStream() {
            return new ByteArrayInputStream(content);
        }
    }
}
