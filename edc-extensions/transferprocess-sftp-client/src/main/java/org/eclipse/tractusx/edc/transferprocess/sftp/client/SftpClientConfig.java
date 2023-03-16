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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.apache.sshd.sftp.client.SftpClient;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpLocation;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpUser;

@Builder
@Getter
public class SftpClientConfig {
  private SftpUser sftpUser;
  private SftpLocation sftpLocation;
  @Builder.Default private int bufferSize = 4096;
  @Builder.Default private boolean hostVerification = true;

  @Builder.Default
  private Path knownHostFile = Paths.get(System.getenv("HOME"), ".ssh/known_hosts");

  @Builder.Default private int connectionTimeoutSeconds = 10;

  @Builder.Default
  private Collection<SftpClient.OpenMode> writeOpenModes =
      List.of(SftpClient.OpenMode.Create, SftpClient.OpenMode.Write);

  @Builder.Default
  private Collection<SftpClient.OpenMode> readOpenModes = List.of(SftpClient.OpenMode.Read);
}
