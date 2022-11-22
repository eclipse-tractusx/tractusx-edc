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

import lombok.RequiredArgsConstructor;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSink;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSinkFactory;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpDataAddress;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpLocation;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpUser;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpUserKeyPairGenerator;
import org.jetbrains.annotations.NotNull;

import java.security.KeyPair;

@RequiredArgsConstructor
public class SftpDataSinkFactory implements DataSinkFactory {

  @NotNull SftpClientWrapper sftpClientWrapper;

  @Override
  public boolean canHandle(DataFlowRequest request) {
    return request.getDestinationDataAddress().getType().equals("sftp")
        && request.getDestinationDataAddress() instanceof SftpDataAddress;
  }

  @Override
  public @NotNull Result<Boolean> validate(DataFlowRequest request) {
    if (request.getDestinationDataAddress().getType().equals("sftp")
        && request.getDestinationDataAddress() instanceof SftpDataAddress) {
      return VALID;
    }
    return Result.failure(String.format("Invalid dataflow request: %s", request.getId()));
  }

  @Override
  public DataSink createSink(DataFlowRequest request) {
    SftpDataAddress destination = (SftpDataAddress) request.getDestinationDataAddress();
    SftpLocation location =
        SftpLocation.builder()
            .host(destination.getSftpLocationHost())
            .port(destination.getSftpLocationPort())
            .path(destination.getSftpLocationPath())
            .build();
    KeyPair sftpUserKeyPair =
        SftpUserKeyPairGenerator.getKeyPairFromPrivateKey(
            destination.getSftpUserPrivateKey(), destination.getSftpUserName());
    SftpUser user =
        SftpUser.builder()
            .name(destination.getSftpUserName())
            .password(destination.getSftpUserPassword())
            .keyPair(sftpUserKeyPair)
            .build();

    return new SftpDataSink(user, location, sftpClientWrapper);
  }
}
