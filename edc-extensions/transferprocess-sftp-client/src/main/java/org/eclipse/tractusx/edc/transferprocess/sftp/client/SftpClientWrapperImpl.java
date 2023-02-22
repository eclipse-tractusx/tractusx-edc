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
import java.time.Duration;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
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
import org.eclipse.tractusx.edc.transferprocess.sftp.common.EdcSftpException;
import org.jetbrains.annotations.NotNull;

public class SftpClientWrapperImpl implements SftpClientWrapper {
  @Getter private final SftpClientConfig config;
  private final SftpClient sftpClient;

  public SftpClientWrapperImpl(SftpClientConfig config) {
    this.config = config;
    try {
      this.sftpClient = getSftpClient(config);
    } catch (IOException e) {
      throw new EdcSftpException("Unable to create SftpClient.", e);
    }
  }

  public SftpClientWrapperImpl(SftpClientConfig config, SftpClient sftpClient) {
    this.config = config;
    this.sftpClient = sftpClient;
  }

  @Override
  public void uploadFile(@NonNull final InputStream inputStream) throws IOException {
    try (final OutputStream outputStream =
        sftpClient.write(
            config.getSftpLocation().getPath(),
            config.getBufferSize(),
            config.getWriteOpenModes())) {
      inputStream.transferTo(outputStream);
    }
  }

  @Override
  public InputStream downloadFile() throws IOException {
    final InputStream delegateInputStream;
    try {
      delegateInputStream =
          sftpClient.read(
              config.getSftpLocation().getPath(),
              config.getBufferSize(),
              config.getReadOpenModes());
    } catch (final IOException e) {
      sftpClient.close();
      throw new EdcSftpException(
          String.format("Unable to download file at %s", config.getSftpLocation().toString()), e);
    }

    return new SftpInputStreamWrapper(sftpClient, delegateInputStream);
  }

  static SftpClient getSftpClient(SftpClientConfig config) throws IOException {
    final ClientSession session = getSshClientSession(config);
    final SftpClientFactory factory = SftpClientFactory.instance();
    final SftpClient sftpClient = factory.createSftpClient(session);
    return sftpClient.singleSessionInstance();
  }

  private static ClientSession getSshClientSession(SftpClientConfig config) throws IOException {
    final SshClient sshClient = getSshClient(config);
    sshClient.start();
    final ClientSession session =
        sshClient
            .connect(
                config.getSftpUser().getName(),
                config.getSftpLocation().getHost(),
                config.getSftpLocation().getPort())
            .verify()
            .getSession();
    session.auth().await(Duration.ofSeconds(config.getConnectionTimeoutSeconds()));

    return session;
  }

  private static SshClient getSshClient(SftpClientConfig config) {
    final SshClient sshClient = ClientBuilder.builder().build();

    if (config.getSftpUser().getKeyPair() != null) {
      sshClient.setKeyIdentityProvider(
          KeyIdentityProvider.wrapKeyPairs(config.getSftpUser().getKeyPair()));
    } else if (config.getSftpUser().getPassword() != null) {
      sshClient.setPasswordIdentityProvider(
          PasswordIdentityProvider.wrapPasswords(config.getSftpUser().getPassword()));
    } else {
      sshClient.setPasswordIdentityProvider(PasswordIdentityProvider.EMPTY_PASSWORDS_PROVIDER);
    }

    if (config.isHostVerification()) {
      final ServerKeyVerifier keyVerifier =
          new KnownHostsServerKeyVerifier(
              RejectAllServerKeyVerifier.INSTANCE, config.getKnownHostFile());
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
        // Ignored. The exception should only be thrown of the stream is already closed.
      }

      try {
        sftpClient.close();
      } catch (IOException ignored) {
        // Ignored. The exception should only be thrown of the client is already closed.
      }
    }
  }
}
