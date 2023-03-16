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

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.sshd.sftp.client.SftpClient;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSinkFactory;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.EdcSftpException;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpDataAddress;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class SftpDataSinkFactory implements DataSinkFactory {
  @Override
  public boolean canHandle(DataFlowRequest request) {
    try {
      SftpDataAddress.fromDataAddress(request.getDestinationDataAddress());
      return true;
    } catch (EdcSftpException e) {
      return false;
    }
  }

  @Override
  public @NotNull Result<Boolean> validate(DataFlowRequest request) {
    if (!canHandle(request)) {
      return Result.failure(String.format("Invalid DataFlowRequest: %s", request.getId()));
    }

    return VALID;
  }

  @Override
  public DataSink createSink(DataFlowRequest request) {
    if (!canHandle(request)) {
      return null;
    }

    SftpDataAddress destination =
        SftpDataAddress.fromDataAddress(request.getDestinationDataAddress());

    SftpClientConfig sftpClientConfig =
        SftpClientConfig.builder()
            .writeOpenModes(List.of(SftpClient.OpenMode.Create, SftpClient.OpenMode.Append))
            .sftpUser(destination.getSftpUser())
            .sftpLocation(destination.getSftpLocation())
            .build();

    SftpClientWrapper sftpClientWrapper = new SftpClientWrapperImpl(sftpClientConfig);

    return new SftpDataSink(sftpClientWrapper);
  }
}
