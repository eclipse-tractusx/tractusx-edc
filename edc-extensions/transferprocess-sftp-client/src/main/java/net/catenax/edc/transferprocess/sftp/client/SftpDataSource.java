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

package net.catenax.edc.transferprocess.sftp.client;

import lombok.Builder;
import lombok.NonNull;
import net.catenax.edc.trasnferprocess.sftp.common.SftpLocation;
import net.catenax.edc.trasnferprocess.sftp.common.SftpUser;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSource;

import java.util.stream.Stream;

@Builder
public class SftpDataSource implements DataSource {
    @NonNull
    private final SftpUser sftpUser;
    @NonNull
    private final SftpLocation sftpLocation;
    @NonNull
    private final SftpClient sftpClient;


    @Override
    public Stream<Part> openPartStream() {
        Part sftpPart = new SftpPart(sftpUser, sftpLocation, sftpClient);
        return Stream.of(sftpPart);
    }
}
