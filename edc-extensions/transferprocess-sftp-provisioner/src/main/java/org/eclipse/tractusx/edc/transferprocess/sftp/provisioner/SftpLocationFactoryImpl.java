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

package org.eclipse.tractusx.edc.transferprocess.sftp.provisioner;

import lombok.RequiredArgsConstructor;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpLocation;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpLocationFactory;

@RequiredArgsConstructor
public class SftpLocationFactoryImpl implements SftpLocationFactory {
  @Override
  public SftpLocation createSftpLocation(String sftpHost, Integer sftpPort, String sftpPath) {
    return SftpLocation.builder().host(sftpHost).port(sftpPort).path(sftpPath).build();
  }
}
