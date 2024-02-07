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
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpDataAddress;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpLocation;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpUser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class SftpDataSourceFactoryTest {

    @Test
    void validate_valid() {
        var dataSourceFactory = new SftpDataSourceFactory();
        var sftpUser = SftpUser.Builder.newInstance().name("name").build();
        var sftpLocation = SftpLocation.Builder.newInstance().host("host").port(22).path("path").build();

        var sftpDataAddress =
                SftpDataAddress.Builder.newInstance().sftpUser(sftpUser).sftpLocation(sftpLocation).build();
        var request = mock(DataFlowRequest.class);
        when(request.getSourceDataAddress()).thenReturn(sftpDataAddress);

        Assertions.assertTrue(dataSourceFactory.validateRequest(request).succeeded());
    }

    @Test
    void validate_invalidDataAddressType() {
        var dataSourceFactory = new SftpDataSourceFactory();
        var dataAddress = DataAddress.Builder.newInstance().type("wrong").build();
        var request = mock(DataFlowRequest.class);
        when(request.getSourceDataAddress()).thenReturn(dataAddress);

        Assertions.assertTrue(dataSourceFactory.validateRequest(request).failed());
    }

    @Test
    void validate_invalidDataAddressParameters() {
        var dataSourceFactory = new SftpDataSourceFactory();
        Map<String, Object> properties =
                Map.of(
                        "type", "sftp",
                        "locationHost", "localhost",
                        "locationPort", "notANumber",
                        "locationPath", "path",
                        "userName", "name",
                        "userPassword", "password");

        var dataAddress = DataAddress.Builder.newInstance().properties(properties).build();
        var request = mock(DataFlowRequest.class);
        when(request.getSourceDataAddress()).thenReturn(dataAddress);

        Assertions.assertTrue(dataSourceFactory.validateRequest(request).failed());
    }

    @Test
    void createSink_successful() {
        var dataSourceFactory = new SftpDataSourceFactory();
        var sftpUser = SftpUser.Builder.newInstance().name("name").build();
        var sftpLocation =
                SftpLocation.Builder.newInstance().host("127.0.0.1").port(22).path("path").build();
        var sftpClientConfig =
                SftpClientConfig.Builder.newInstance().sftpUser(sftpUser).sftpLocation(sftpLocation).build();

        var sftpDataAddress =
                SftpDataAddress.Builder.newInstance().sftpUser(sftpUser).sftpLocation(sftpLocation).build();
        var request = mock(DataFlowRequest.class);
        when(request.getSourceDataAddress()).thenReturn(sftpDataAddress);

        try (var staticWrapper =
                     mockStatic(SftpClientWrapperImpl.class)) {
            staticWrapper
                    .when(() -> SftpClientWrapperImpl.getSftpClient(sftpClientConfig))
                    .thenReturn(mock(SftpClient.class));
            Assertions.assertNotNull(dataSourceFactory.createSource(request));

            staticWrapper.verify(
                    () -> SftpClientWrapperImpl.getSftpClient(any()), times(1));
        }
    }

    @Test
    void createSink_invalidDataAddressType() {
        var dataSourceFactory = new SftpDataSourceFactory();
        var dataAddress = DataAddress.Builder.newInstance().type("wrong").build();
        var request = mock(DataFlowRequest.class);
        when(request.getSourceDataAddress()).thenReturn(dataAddress);

        Assertions.assertNull(dataSourceFactory.createSource(request));
    }
}
