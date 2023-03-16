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

import java.security.KeyPair;
import lombok.Builder;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpUser;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpUserFactory;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpUserKeyPairGenerator;

@Builder
public class SftpUserFactoryImpl implements SftpUserFactory {

  @Override
  public SftpUser createSftpUser(
      String sftpUserName, String sftpUserPassword, byte[] sftpUserPrivateKey) {
    KeyPair sftpUserKeyPair =
        SftpUserKeyPairGenerator.getKeyPairFromPrivateKey(sftpUserPrivateKey, sftpUserName);

    return SftpUser.builder()
        .name(sftpUserName)
        .password(sftpUserPassword)
        .keyPair(sftpUserKeyPair)
        .build();
  }
}
