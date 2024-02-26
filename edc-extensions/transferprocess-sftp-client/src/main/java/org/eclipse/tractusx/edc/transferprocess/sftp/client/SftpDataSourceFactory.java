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

import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.EdcSftpException;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpDataAddress;
import org.jetbrains.annotations.NotNull;

public class SftpDataSourceFactory implements DataSourceFactory {
    @Override
    public boolean canHandle(DataFlowStartMessage request) {
        try {
            SftpDataAddress.fromDataAddress(request.getSourceDataAddress());
            return true;
        } catch (EdcSftpException e) {
            return false;
        }
    }

    @Override
    public DataSource createSource(DataFlowStartMessage request) {
        if (!canHandle(request)) {
            return null;
        }

        SftpDataAddress source = SftpDataAddress.fromDataAddress(request.getSourceDataAddress());

        SftpClientConfig sftpClientConfig =
                SftpClientConfig.Builder.newInstance()
                        .sftpUser(source.getSftpUser())
                        .sftpLocation(source.getSftpLocation())
                        .build();

        SftpClientWrapper sftpClientWrapper = new SftpClientWrapperImpl(sftpClientConfig);
        return new SftpDataSource(sftpClientWrapper);
    }

    @Override
    public @NotNull Result<Void> validateRequest(DataFlowStartMessage request) {
        if (!canHandle(request)) {
            return Result.failure(String.format("Invalid DataFlowStartMessage: %s", request.getId()));
        }
        return Result.success();
    }
}
