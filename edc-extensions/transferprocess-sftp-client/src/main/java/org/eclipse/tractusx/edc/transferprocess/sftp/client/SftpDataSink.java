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

import java.io.IOException;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.util.sink.ParallelSink;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;

@Builder
public class SftpDataSink extends ParallelSink {
  @NonNull private final SftpClientWrapper sftpClientWrapper;

  @Override
  protected StatusResult<Void> transferParts(List<DataSource.Part> parts) {
    for (DataSource.Part part : parts) {
      try {
        sftpClientWrapper.uploadFile(part.openStream());
      } catch (IOException e) {
        return StatusResult.failure(ResponseStatus.FATAL_ERROR, e.getMessage());
      }
    }
    return StatusResult.success();
  }
}
