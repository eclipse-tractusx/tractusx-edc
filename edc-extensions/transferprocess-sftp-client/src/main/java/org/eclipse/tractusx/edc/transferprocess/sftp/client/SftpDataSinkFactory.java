/********************************************************************************
 * Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
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
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSinkFactory;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.EdcSftpException;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpDataAddress;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SftpDataSinkFactory implements DataSinkFactory {
    @Override
    public boolean canHandle(DataFlowStartMessage request) {
        try {
            SftpDataAddress.fromDataAddress(request.getDestinationDataAddress());
            return true;
        } catch (EdcSftpException e) {
            return false;
        }
    }

    @Override
    public DataSink createSink(DataFlowStartMessage request) {
        if (!canHandle(request)) {
            return null;
        }

        SftpDataAddress destination =
                SftpDataAddress.fromDataAddress(request.getDestinationDataAddress());

        SftpClientConfig sftpClientConfig =
                SftpClientConfig.Builder.newInstance()
                        .writeOpenModes(List.of(SftpClient.OpenMode.Create, SftpClient.OpenMode.Append))
                        .sftpUser(destination.getSftpUser())
                        .sftpLocation(destination.getSftpLocation())
                        .build();

        SftpClientWrapper sftpClientWrapper = new SftpClientWrapperImpl(sftpClientConfig);

        return new SftpDataSink(sftpClientWrapper);
    }

    @Override
    public @NotNull Result<Void> validateRequest(DataFlowStartMessage request) {
        if (!canHandle(request)) {
            return Result.failure(String.format("Invalid DataFlowStartMessage: %s", request.getId()));
        }

        return Result.success();
    }
}
