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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.catenax.edc.transferprocess.sftp.provisioner.SftpLocation;
import net.catenax.edc.transferprocess.sftp.provisioner.SftpUser;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.OpenMode;
import net.schmizz.sshj.sftp.RemoteFile;
import net.schmizz.sshj.sftp.SFTPClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;

public class SftpClientImpl implements SftpClient {
    private static final int EOF = -1;
    private static final int BUFFER_SIZE = 4096;

    @Override
    public void uploadFile(@NonNull final SftpUser sftpUser, @NonNull final SftpLocation sftpLocation, @NonNull final InputStream inputStream) throws IOException {
        try (final SSHClient ssh = new SSHClient()) {
            ssh.loadKnownHosts();
            ssh.connect(String.format("%s:%d", sftpLocation.getHost(), sftpLocation.getPort()));
            ssh.authPassword(sftpUser.getName(), Arrays.toString(sftpUser.getKey()).toCharArray());

            final SFTPClient sftp = ssh.newSFTPClient();
            try (final RemoteFile remoteFile = sftp.open(sftpLocation.getPath(), new HashSet<>(Arrays.asList(OpenMode.CREAT, OpenMode.WRITE, OpenMode.TRUNC)))) {
                int value = EOF;
                int chunks = -1;
                do {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

                    for (int i = 0; i <= BUFFER_SIZE; i++) {
                        value = inputStream.read();
                        if (value == EOF) {
                            break;
                        }
                        byteArrayOutputStream.write(value);
                    }
                    byteArrayOutputStream.flush();
                    byte[] buffer = byteArrayOutputStream.toByteArray();
                    remoteFile.write((++chunks) * BUFFER_SIZE, buffer, 0, buffer.length);
                } while (value != EOF);
            }
        }
    }

    @Override
    public InputStream downloadFile(@NonNull final SftpUser sftpUser, @NonNull final SftpLocation sftpLocation) throws IOException {
        final SSHClient ssh = new SSHClient();
        ssh.loadKnownHosts();
        ssh.connect(String.format("%s:%d", sftpLocation.getHost(), sftpLocation.getPort()));
        ssh.authPassword(sftpUser.getName(), Arrays.toString(sftpUser.getKey()).toCharArray());

        final SFTPClient sftp = ssh.newSFTPClient();
        final RemoteFile remoteFile = sftp.open(sftpLocation.getPath());
        return new DownloadInputStream(ssh, remoteFile);

    }

    @RequiredArgsConstructor
    private static class DownloadInputStream extends InputStream {
        @NonNull
        private final SSHClient sshClient;
        @NonNull
        private final RemoteFile remoteFile;

        private final byte[] buffer = new byte[BUFFER_SIZE];
        private boolean eofReached = false;
        private int bufferIndex = -1;
        private long remoteFileIndex = -1;

        @Override
        public int read() throws IOException {
            if (remoteFileIndex == -1 || bufferIndex >= buffer.length) {
                if (eofReached) {
                    return EOF;
                }
                nextChunkToBuffer();
            }
            return buffer[++bufferIndex];
        }

        private void nextChunkToBuffer() throws IOException {
            if (eofReached) {
                return;
            }
            int bytesReceived = remoteFile.read((++remoteFileIndex) * BUFFER_SIZE, buffer, 0, BUFFER_SIZE);
            bufferIndex = -1;
            if (bytesReceived == EOF) {
                eofReached = true;
            }
        }

        @Override
        public void close() {
            try {
                remoteFile.close();
            } catch (Throwable ignored) {
            }
            try {
                sshClient.close();
            } catch (Throwable ignored) {
            }
        }
    }
}
