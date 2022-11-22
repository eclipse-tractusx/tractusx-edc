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

import java.security.KeyPair;
import lombok.RequiredArgsConstructor;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSource;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpDataAddress;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpLocation;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpUser;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpUserKeyPairGenerator;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class SftpDataSourceFactory implements DataSourceFactory {

  @NotNull SftpClientWrapper sftpClientWrapper;

  @Override
  public boolean canHandle(DataFlowRequest request) {
    return request.getSourceDataAddress().getType().equals("sftp")
        && request.getSourceDataAddress() instanceof SftpDataAddress;
  }

  @Override
  public @NotNull Result<Boolean> validate(DataFlowRequest request) {
    if (request.getSourceDataAddress().getType().equals("sftp")
        && request.getSourceDataAddress() instanceof SftpDataAddress) {
      return VALID;
    }
    return Result.failure(String.format("Invalid dataflow request: %s", request.getId()));
  }

  @Override
  public DataSource createSource(DataFlowRequest request) {
    SftpDataAddress source = (SftpDataAddress) request.getSourceDataAddress();
    SftpLocation location =
        SftpLocation.builder()
            .host(source.getSftpLocationHost())
            .port(source.getSftpLocationPort())
            .path(source.getSftpLocationPath())
            .build();
    KeyPair sftpUserKeyPair =
        SftpUserKeyPairGenerator.getKeyPairFromPrivateKey(
            source.getSftpUserPrivateKey(), source.getSftpUserName());
    SftpUser user =
        SftpUser.builder()
            .name(source.getSftpUserName())
            .password(source.getSftpUserPassword())
            .keyPair(sftpUserKeyPair)
            .build();

    return new SftpDataSource(user, location, sftpClientWrapper);
  }
}
