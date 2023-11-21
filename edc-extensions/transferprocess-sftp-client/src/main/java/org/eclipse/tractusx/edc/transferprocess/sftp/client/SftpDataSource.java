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

import java.util.stream.Stream;

public class SftpDataSource implements DataSource {
    private final SftpClientWrapper sftpClientWrapper;

    public SftpDataSource(SftpClientWrapper sftpClientWrapper) {
        this.sftpClientWrapper = sftpClientWrapper;
    }


    @Override
    public StreamResult<Stream<Part>> openPartStream() {
        Part sftpPart = new SftpPart(sftpClientWrapper);
        return StreamResult.success(Stream.of(sftpPart));
    }

    @Override
    public void close() throws Exception {
    }
}
