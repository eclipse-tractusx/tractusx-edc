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

package net.catenax.edc.transferprocess.sftp.provisioner;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;

import java.io.IOException;

public class GenericSftpProvider implements SftpProvider {
    @Override
    public void createUser(SftpUser user) {

    }

    @Override
    public void deleteUser(SftpUser user) {

    }

    @Override
    public void createLocation(SftpLocation location) {
        String remotePath = String.format("sfpt://%s:%d/%s",
                location.getHost(),
                location.getPort(),
                location.getPath());

        SSHClient sshClient = new SSHClient();
        try {
            sshClient.loadKnownHosts();
            sshClient.connect(remotePath);
            SFTPClient sftpClient = sshClient.newSFTPClient();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteLocation(SftpLocation location) {

    }
}
