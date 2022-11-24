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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import org.apache.sshd.sftp.client.SftpClient;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpLocation;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpUser;

public interface SftpClientWrapper {
  void uploadFile(
      SftpUser sftpUser,
      SftpLocation sftpLocation,
      InputStream inputStream,
      Collection<SftpClient.OpenMode> openModes)
      throws IOException;

  void uploadFile(SftpUser sftpUser, SftpLocation sftpLocation, InputStream inputStream)
      throws IOException;

  InputStream downloadFile(SftpUser sftpUser, SftpLocation sftpLocation) throws IOException;
}
