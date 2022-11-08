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

import lombok.Builder;
import lombok.NonNull;
import org.eclipse.tractusx.edc.trasnferprocess.sftp.common.SftpUser;
import org.eclipse.tractusx.edc.trasnferprocess.sftp.common.SftpUserFactory;
import org.eclipse.dataspaceconnector.spi.EdcException;

import java.security.KeyPair;

@Builder
public class ConfigBackedSftpUserFactory implements SftpUserFactory {
    @NonNull
    private final String sftpUserName;
    private final String sftpUserPassword;
    private final KeyPair sftpUserKeyPair;

    @Override
    public SftpUser createSftpUser(String transferProcessId) {
        if (sftpUserKeyPair != null) {
            return SftpUser.builder().name(sftpUserName).keyPair(sftpUserKeyPair).build();
        }
        if (sftpUserPassword != null) {
            return SftpUser.builder().name(sftpUserName).password(sftpUserPassword).build();
        }
        throw new EdcException(String.format("No auth method provided for SftpUser %s", sftpUserName));
    }
}
