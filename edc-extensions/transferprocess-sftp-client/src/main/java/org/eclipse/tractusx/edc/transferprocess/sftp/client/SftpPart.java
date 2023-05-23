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
 *       Mercedes-Benz Tech Innovation GmbH - Initial API and Implementation
 *
 */

package org.eclipse.tractusx.edc.transferprocess.sftp.client;

import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.EdcSftpException;

import java.io.IOException;
import java.io.InputStream;

public class SftpPart implements DataSource.Part {
    private final SftpClientWrapper sftpClientWrapper;

    public SftpPart(SftpClientWrapper sftpClientWrapper) {
        this.sftpClientWrapper = sftpClientWrapper;
    }

    @Override
    public String name() {
        return ((SftpClientWrapperImpl) sftpClientWrapper).getConfig().getSftpLocation().getPath();
    }

    @Override
    public InputStream openStream() {
        try {
            return sftpClientWrapper.downloadFile();
        } catch (IOException e) {
            throw new EdcSftpException(e);
        }
    }
}
