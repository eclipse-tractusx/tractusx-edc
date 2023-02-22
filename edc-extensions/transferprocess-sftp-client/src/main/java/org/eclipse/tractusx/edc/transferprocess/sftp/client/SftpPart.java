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

import java.io.InputStream;
import lombok.Builder;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;

@Builder
public class SftpPart implements DataSource.Part {
  @NonNull private final SftpClientWrapper sftpClientWrapper;

  @Override
  public String name() {
    return ((SftpClientWrapperImpl) sftpClientWrapper).getConfig().getSftpLocation().getPath();
  }

  @Override
  @SneakyThrows
  public InputStream openStream() {
    return sftpClientWrapper.downloadFile();
  }
}
