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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import net.catenax.edc.transferprocess.sftp.provisioner.SftpLocation;
import net.catenax.edc.transferprocess.sftp.provisioner.SftpUser;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.OpenMode;
import net.schmizz.sshj.sftp.RemoteFile;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.UserAuthException;
import org.eclipse.dataspaceconnector.spi.EdcException;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;

public class SftpClientImpl implements SftpClient {
    private static final int EOF = -1;
    private static final int BUFFER_SIZE_DEFAULT = 4096;

    @Setter
    private boolean disableHostVerification = false;
    @Setter
    private Path knownHostFile = Paths.get(System.getenv("HOME"), ".ssh/known_hosts");

    @Override
    public void uploadFile(@NonNull final SftpUser sftpUser, @NonNull final SftpLocation sftpLocation, @NonNull final InputStream inputStream) throws IOException {
        try (final SSHClient ssh = getSshClient()) {
            ssh.connect(sftpLocation.getHost(),sftpLocation.getPort());
            authenticate(sftpUser, ssh);

            final SFTPClient sftp = ssh.newSFTPClient();
            try (final RemoteFile remoteFile = sftp.open(sftpLocation.getPath(), new HashSet<>(Arrays.asList(OpenMode.CREAT, OpenMode.WRITE, OpenMode.TRUNC)))) {
                int value = EOF;
                int chunks = -1;
                do {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

                    for (int i = 0; i < BUFFER_SIZE_DEFAULT; i++) {
                        value = inputStream.read();
                        if (value == EOF) {
                            break;
                        }
                        byteArrayOutputStream.write(value);
                    }
                    byteArrayOutputStream.flush();
                    byte[] buffer = byteArrayOutputStream.toByteArray();
                    remoteFile.write((++chunks) * BUFFER_SIZE_DEFAULT, buffer, 0, buffer.length);
                } while (value != EOF);
            }
        }
    }

    @Override
    public InputStream downloadFile(@NonNull final SftpUser sftpUser, @NonNull final SftpLocation sftpLocation) throws IOException {
        final SSHClient ssh = getSshClient();
        ssh.connect(sftpLocation.getHost(),sftpLocation.getPort());
        authenticate(sftpUser, ssh);

        final SFTPClient sftp = ssh.newSFTPClient();
        final RemoteFile remoteFile = sftp.open(sftpLocation.getPath());
        return new DownloadInputStream(ssh, remoteFile);

    }

    @SneakyThrows
    private SSHClient getSshClient() {
        final SSHClient ssh = new SSHClient();
        if (disableHostVerification) {
            ssh.addHostKeyVerifier(new PromiscuousVerifier());
        } else {
            ssh.loadKnownHosts(knownHostFile.toFile());
        }

        return ssh;
    }

    private void authenticate(SftpUser sftpUser, SSHClient ssh) throws UserAuthException, TransportException {
        if (sftpUser.getKeyPair() != null) {
            ssh.loadKeys(sftpUser.getKeyPair());
            ssh.authPublickey(sftpUser.getName());
        } else if (sftpUser.getPassword() != null)
        {
            char[] password = sftpUser.getPassword().toCharArray();
            ssh.authPassword(sftpUser.getName(), password);
        } else {
            throw new EdcException(String.format("No auth method provided for SftpUser %s", sftpUser.getName()));
        }
    }

    @RequiredArgsConstructor
    private static class DownloadInputStream extends InputStream {
        @NonNull
        private final SSHClient sshClient;
        @NonNull
        private final RemoteFile remoteFile;

        private byte[] buffer;
        private int bufferIndex = 0;
        private long remoteFileBytesAlreadyRead = 0;
        private int bufferSize = BUFFER_SIZE_DEFAULT;
        @Getter(lazy = true, value = AccessLevel.PRIVATE)
        private final long remoteFileLength = getRemoteFileLengthLazy();

        @Override
        public int read() throws IOException {
            long remoteFileLength = getRemoteFileLength();

            if (bufferIndex >= bufferSize || remoteFileBytesAlreadyRead == 0) {
                if (remoteFileBytesAlreadyRead + BUFFER_SIZE_DEFAULT <= remoteFileLength) {
                    buffer = new byte[bufferSize];
                    remoteFileBytesAlreadyRead += nextChunkToBuffer(buffer, remoteFileBytesAlreadyRead);
                } else if (remoteFileBytesAlreadyRead < remoteFileLength) {
                    bufferSize = (int) (remoteFileLength - remoteFileBytesAlreadyRead);
                    buffer = new byte[bufferSize];
                    remoteFileBytesAlreadyRead += nextChunkToBuffer(buffer, remoteFileBytesAlreadyRead);
                } else {
                    return EOF;
                }
                bufferIndex = 0;
            }

            int data = Byte.toUnsignedInt(buffer[bufferIndex]);
            bufferIndex++;
            return data;
        }

        private int nextChunkToBuffer(byte[] buffer, long fileOffset) throws IOException {
            return remoteFile.read(fileOffset, buffer, 0, buffer.length);
        }

        @Override
        public int available() {
            return (int) (getRemoteFileLength() - remoteFileBytesAlreadyRead);
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

        @SneakyThrows
        private long getRemoteFileLengthLazy() {
            return remoteFile.length();
        }
    }
}
