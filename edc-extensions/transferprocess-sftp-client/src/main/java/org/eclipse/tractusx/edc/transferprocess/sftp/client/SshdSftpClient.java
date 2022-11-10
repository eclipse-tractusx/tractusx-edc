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

import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.sshd.client.ClientBuilder;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.auth.password.PasswordIdentityProvider;
import org.apache.sshd.client.keyverifier.KnownHostsServerKeyVerifier;
import org.apache.sshd.client.keyverifier.RejectAllServerKeyVerifier;
import org.apache.sshd.client.keyverifier.ServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.keyprovider.KeyIdentityProvider;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.eclipse.tractusx.edc.trasnferprocess.sftp.common.SftpLocation;
import org.eclipse.tractusx.edc.trasnferprocess.sftp.common.SftpUser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

public class SshdSftpClient implements SftpClientWrapper {

  @Setter private int bufferSize = 4096;
  @Setter private boolean disableHostVerification = false;
  @Setter private Path knownHostFile = Paths.get(System.getenv("HOME"), ".ssh/known_hosts");

  @Override
  public void uploadFile(
      @NonNull final SftpUser sftpUser,
      @NonNull final SftpLocation sftpLocation,
      @NonNull final InputStream inputStream)
      throws IOException {
    try (final SftpClient sftpClient = getSftpClient(sftpUser, sftpLocation)) {
      try (OutputStream outputStream =
          sftpClient.write(sftpLocation.getPath(), bufferSize)) {
        inputStream.transferTo(outputStream);
      }
    }
  }

  @Override
  public InputStream downloadFile(
      @NonNull final SftpUser sftpUser, @NonNull final SftpLocation sftpLocation)
      throws IOException {
    return getSftpClient(sftpUser, sftpLocation).read(sftpLocation.getPath(), bufferSize);
  }

  @SneakyThrows
  private SftpClient getSftpClient(SftpUser sftpUser, SftpLocation sftpLocation) {
    SshClient sshClient = ClientBuilder.builder().build();
    if (sftpUser.getKeyPair() != null) {
      sshClient.setKeyIdentityProvider(KeyIdentityProvider.wrapKeyPairs(sftpUser.getKeyPair()));
    } else if (sftpUser.getPassword() != null) {
      sshClient.setPasswordIdentityProvider(
          PasswordIdentityProvider.wrapPasswords(sftpUser.getPassword()));
    } else {
      sshClient.setPasswordIdentityProvider(PasswordIdentityProvider.EMPTY_PASSWORDS_PROVIDER);
    }

    if (!disableHostVerification) {
      final ServerKeyVerifier keyVerifier = new KnownHostsServerKeyVerifier(RejectAllServerKeyVerifier.INSTANCE, knownHostFile);
      sshClient.setServerKeyVerifier(keyVerifier);
    }

    sshClient.start();

    ClientSession session =
        sshClient
            .connect(sftpUser.getName(), sftpLocation.getHost(), sftpLocation.getPort())
            .verify()
            .getSession();
    // TODO: set timeout
    session.auth().await(Duration.ofSeconds(10));

    SftpClientFactory factory = SftpClientFactory.instance();
    SftpClient sftpClient = factory.createSftpClient(session);
    return sftpClient.singleSessionInstance();
  }
}
