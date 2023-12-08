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
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.connector.dataplane.util.sink.ParallelSink;

import java.io.IOException;
import java.util.List;

public class SftpDataSink extends ParallelSink {
    private final SftpClientWrapper sftpClientWrapper;

    public SftpDataSink(SftpClientWrapper sftpClientWrapper) {
        this.sftpClientWrapper = sftpClientWrapper;
    }


    @Override
    protected StreamResult<Object> transferParts(List<DataSource.Part> parts) {
        for (DataSource.Part part : parts) {
            try {
                sftpClientWrapper.uploadFile(part.openStream());
            } catch (IOException e) {
                return StreamResult.error(e.getMessage());
            }
        }
        return StreamResult.success();
    }
}
