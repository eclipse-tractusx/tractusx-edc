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

package net.catenax.edc.transferprocess.sftp.client;

import net.catenax.edc.trasnferprocess.sftp.common.SftpLocation;
import net.catenax.edc.trasnferprocess.sftp.common.SftpUser;

import java.io.IOException;
import java.io.InputStream;

public interface SftpClient {
    void uploadFile(SftpUser sftpUser, SftpLocation sftpLocation, InputStream inputStream) throws IOException;

    InputStream downloadFile(SftpUser sftpUser, SftpLocation sftpLocation) throws IOException;
}
