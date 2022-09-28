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

package net.catenax.edc.transferprocess.sftp.client;

import lombok.Builder;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.catenax.edc.transferprocess.sftp.provisioner.SftpLocation;
import net.catenax.edc.transferprocess.sftp.provisioner.SftpUser;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSource;

import java.io.InputStream;

@Builder
public class SftpPart implements DataSource.Part {
    @NonNull
    private final SftpUser sftpUser;
    @NonNull
    private final SftpLocation sftpLocation;
    @NonNull
    private final SftpClient sftpClient;
    @Override
    public String name() {
        return sftpLocation.getPath();
    }

    @Override
    @SneakyThrows
    public InputStream openStream() {
        return sftpClient.downloadFile(sftpUser, sftpLocation);
    }
}
