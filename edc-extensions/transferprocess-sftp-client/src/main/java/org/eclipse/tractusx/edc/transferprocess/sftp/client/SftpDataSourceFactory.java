/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Initial Test
 *
 */

package org.eclipse.tractusx.edc.transferprocess.sftp.client;

import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.EdcSftpException;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpDataAddress;
import org.jetbrains.annotations.NotNull;

public class SftpDataSourceFactory implements DataSourceFactory {
    @Override
    public boolean canHandle(DataFlowRequest request) {
        try {
            SftpDataAddress.fromDataAddress(request.getSourceDataAddress());
            return true;
        } catch (EdcSftpException e) {
            return false;
        }
    }

    @Override
    public DataSource createSource(DataFlowRequest request) {
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
    public @NotNull Result<Void> validateRequest(DataFlowRequest request) {
        if (!canHandle(request)) {
            return Result.failure(String.format("Invalid DataFlowRequest: %s", request.getId()));
        }
        return Result.success();
    }
}
