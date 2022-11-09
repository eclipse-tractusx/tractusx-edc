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
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSource;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.ParallelSink;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;

public class SftpDataSink extends ParallelSink {

  @Override
  protected StatusResult<Void> transferParts(List<DataSource.Part> parts) {
    return null;
  }
}
