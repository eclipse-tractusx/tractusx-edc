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
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
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
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpLocation;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpUser;
import org.jetbrains.annotations.NotNull;

public class SftpClientWrapperImpl implements SftpClientWrapper {
  private static final int DEFAULT_BUFFER_SIZE = 4096;
  private static final boolean DEFAULT_DISABLE_HOST_VERIFICATION = false;
  private static final Path DEFAULT_KNOWN_HOST_FILE =
      Paths.get(System.getenv("HOME"), ".ssh/known_hosts");
  private static final int DEFAULT_CONNECTION_TIMEOUT_SECONDS = 10;
  private static final Collection<SftpClient.OpenMode> DEFAULT_UPLOAD_OPEN_MODES =
      List.of(SftpClient.OpenMode.Create, SftpClient.OpenMode.Write);

  @Setter private int bufferSize = DEFAULT_BUFFER_SIZE;
  @Setter private boolean disableHostVerification = DEFAULT_DISABLE_HOST_VERIFICATION;
  @Setter private Path knownHostFile = DEFAULT_KNOWN_HOST_FILE;
  @Setter private int connectionTimeoutSeconds = DEFAULT_CONNECTION_TIMEOUT_SECONDS;

  @Override
  public void uploadFile(
      @NonNull final SftpUser sftpUser,
      @NonNull final SftpLocation sftpLocation,
      @NonNull final InputStream inputStream,
      @NonNull Collection<SftpClient.OpenMode> openModes)
      throws IOException {
    try (final SftpClient sftpClient = getSftpClient(sftpUser, sftpLocation)) {
      try (final OutputStream outputStream =
          sftpClient.write(sftpLocation.getPath(), bufferSize, openModes)) {
        inputStream.transferTo(outputStream);
      }
    }
  }

  @Override
  public void uploadFile(
      @NonNull final SftpUser sftpUser,
      @NonNull final SftpLocation sftpLocation,
      @NonNull final InputStream inputStream)
      throws IOException {
    uploadFile(sftpUser, sftpLocation, inputStream, DEFAULT_UPLOAD_OPEN_MODES);
  }

  @Override
  public InputStream downloadFile(
      @NonNull final SftpUser sftpUser, @NonNull final SftpLocation sftpLocation)
      throws IOException {
    final SftpClient sftpClient = getSftpClient(sftpUser, sftpLocation);

    final InputStream delegateInputStream;
    try {
      delegateInputStream = sftpClient.read(sftpLocation.getPath(), bufferSize);
    } catch (final IOException ioException) {
      sftpClient.close();

      throw ioException;
    }

    return new SftpInputStreamWrapper(sftpClient, delegateInputStream);
  }

  SftpClient getSftpClient(
      @NonNull final SftpUser sftpUser, @NonNull final SftpLocation sftpLocation)
      throws IOException {

    final ClientSession session = getSshClientSession(sftpUser, sftpLocation);

    final SftpClientFactory factory = SftpClientFactory.instance();
    final SftpClient sftpClient = factory.createSftpClient(session);
    return sftpClient.singleSessionInstance();
  }

  private ClientSession getSshClientSession(
      @NonNull final SftpUser sftpUser, @NonNull final SftpLocation sftpLocation)
      throws IOException {
    final SshClient sshClient = getSshClient(sftpUser);

    sshClient.start();

    final ClientSession session =
        sshClient
            .connect(sftpUser.getName(), sftpLocation.getHost(), sftpLocation.getPort())
            .verify()
            .getSession();
    session.auth().await(Duration.ofSeconds(connectionTimeoutSeconds));

    return session;
  }

  private SshClient getSshClient(@NonNull final SftpUser sftpUser) {
    final SshClient sshClient = ClientBuilder.builder().build();

    if (sftpUser.getKeyPair() != null) {
      sshClient.setKeyIdentityProvider(KeyIdentityProvider.wrapKeyPairs(sftpUser.getKeyPair()));
    } else if (sftpUser.getPassword() != null) {
      sshClient.setPasswordIdentityProvider(
          PasswordIdentityProvider.wrapPasswords(sftpUser.getPassword()));
    } else {
      sshClient.setPasswordIdentityProvider(PasswordIdentityProvider.EMPTY_PASSWORDS_PROVIDER);
    }

    if (!disableHostVerification) {
      final ServerKeyVerifier keyVerifier =
          new KnownHostsServerKeyVerifier(RejectAllServerKeyVerifier.INSTANCE, knownHostFile);
      sshClient.setServerKeyVerifier(keyVerifier);
    }

    return sshClient;
  }

  @RequiredArgsConstructor
  private static class SftpInputStreamWrapper extends InputStream {
    @NonNull private final SftpClient sftpClient;
    @NonNull private final InputStream delegateInputStream;

    @Override
    public int read() throws IOException {
      return delegateInputStream.read();
    }

    @Override
    public int read(byte @NotNull [] b, int off, int len) throws IOException {
      return delegateInputStream.read(b, off, len);
    }

    @Override
    public void close() {
      try {
        delegateInputStream.close();
      } catch (IOException ignored) {
      }

      try {
        sftpClient.close();
      } catch (IOException ignored) {
      }
    }
  }
}
