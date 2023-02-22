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

package org.eclipse.tractusx.edc.transferprocess.sftp.common;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Map;
import lombok.SneakyThrows;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SftpDataAddressTest {

  @Test
  void fromDataAddress__password() {
    final Map<String, String> properties =
        Map.of(
            "type", "sftp",
            "locationHost", "localhost",
            "locationPort", "22",
            "locationPath", "path",
            "userName", "name",
            "userPassword", "password");

    final DataAddress dataAddress =
        DataAddress.Builder.newInstance().properties(properties).build();

    final SftpDataAddress sftpDataAddress = SftpDataAddress.fromDataAddress(dataAddress);

    Assertions.assertEquals("localhost", sftpDataAddress.getSftpLocation().getHost());
    Assertions.assertEquals(22, sftpDataAddress.getSftpLocation().getPort());
    Assertions.assertEquals("path", sftpDataAddress.getSftpLocation().getPath());
    Assertions.assertEquals("name", sftpDataAddress.getSftpUser().getName());
    Assertions.assertEquals("password", sftpDataAddress.getSftpUser().getPassword());
    Assertions.assertNull(sftpDataAddress.getSftpUser().getKeyPair());
  }

  @Test
  @SneakyThrows
  void fromDataAddress__keyPair() {
    final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    keyPairGenerator.initialize(2048);
    final KeyPair keyPair = keyPairGenerator.generateKeyPair();
    final byte[] privateKeyBytes = keyPair.getPrivate().getEncoded();

    final Map<String, String> properties =
        Map.of(
            "type", "sftp",
            "locationHost", "localhost",
            "locationPort", "22",
            "locationPath", "path",
            "userName", "name",
            "userPrivateKey", Base64.getEncoder().encodeToString(privateKeyBytes));

    final DataAddress dataAddress =
        DataAddress.Builder.newInstance().properties(properties).build();

    final SftpDataAddress sftpDataAddress = SftpDataAddress.fromDataAddress(dataAddress);

    Assertions.assertEquals("localhost", sftpDataAddress.getSftpLocation().getHost());
    Assertions.assertEquals(22, sftpDataAddress.getSftpLocation().getPort());
    Assertions.assertEquals("path", sftpDataAddress.getSftpLocation().getPath());
    Assertions.assertEquals("name", sftpDataAddress.getSftpUser().getName());
    Assertions.assertArrayEquals(
        privateKeyBytes, sftpDataAddress.getSftpUser().getKeyPair().getPrivate().getEncoded());
    Assertions.assertNull(sftpDataAddress.getSftpUser().getPassword());
  }

  @Test
  @SneakyThrows
  void fromDataAddress__noAuth() {
    final Map<String, String> properties =
        Map.of(
            "type", "sftp",
            "locationHost", "localhost",
            "locationPort", "22",
            "locationPath", "path",
            "userName", "name");

    final DataAddress dataAddress =
        DataAddress.Builder.newInstance().properties(properties).build();

    final SftpDataAddress sftpDataAddress = SftpDataAddress.fromDataAddress(dataAddress);

    Assertions.assertEquals("localhost", sftpDataAddress.getSftpLocation().getHost());
    Assertions.assertEquals(22, sftpDataAddress.getSftpLocation().getPort());
    Assertions.assertEquals("path", sftpDataAddress.getSftpLocation().getPath());
    Assertions.assertEquals("name", sftpDataAddress.getSftpUser().getName());
    Assertions.assertNull(sftpDataAddress.getSftpUser().getPassword());
  }

  @Test
  @SneakyThrows
  void fromDataAddress__invalidKeyPairBrokenBase64() {
    final Map<String, String> properties =
        Map.of(
            "type", "sftp",
            "locationHost", "localhost",
            "locationPort", "22",
            "locationPath", "path",
            "userName", "name",
            "userPrivateKey", "clearlyNotAPrivateKey");

    final DataAddress dataAddress =
        DataAddress.Builder.newInstance().properties(properties).build();

    EdcSftpException edcSftpException =
        Assertions.assertThrows(
            EdcSftpException.class, () -> SftpDataAddress.fromDataAddress(dataAddress));
    Assertions.assertEquals(
        "Cannot decode base64 private key for user name", edcSftpException.getMessage());
  }

  @Test
  @SneakyThrows
  void fromDataAddress__invalidKeyPairButCorrectBase64() {
    final Map<String, String> properties =
        Map.of(
            "type", "sftp",
            "locationHost", "localhost",
            "locationPort", "22",
            "locationPath", "path",
            "userName", "name",
            "userPrivateKey", "TWFueSBoYW5kcyBtYWtlIGxpZ2h0IHdvcmsu");

    final DataAddress dataAddress =
        DataAddress.Builder.newInstance().properties(properties).build();

    EdcSftpException edcSftpException =
        Assertions.assertThrows(
            EdcSftpException.class, () -> SftpDataAddress.fromDataAddress(dataAddress));
    Assertions.assertEquals(
        "Unable to parse provided keypair for Sftp user name", edcSftpException.getMessage());
  }

  @Test
  void fromDataAddress__portNaN() {
    final Map<String, String> properties =
        Map.of(
            "type", "sftp",
            "locationHost", "localhost",
            "locationPort", "notANumber",
            "locationPath", "path",
            "userName", "name",
            "userPassword", "password");

    final DataAddress dataAddress =
        DataAddress.Builder.newInstance().properties(properties).build();

    EdcSftpException edcSftpException =
        Assertions.assertThrows(
            EdcSftpException.class, () -> SftpDataAddress.fromDataAddress(dataAddress));
    Assertions.assertEquals(
        "Port for SftpLocation localhost/path not a number", edcSftpException.getMessage());
  }

  @Test
  void fromDataAddress__missingParameter() {
    final Map<String, String> properties =
        Map.of(
            "type", "sftp",
            "locationPort", "22",
            "locationPath", "path",
            "userName", "name",
            "userPassword", "password");

    final DataAddress dataAddress =
        DataAddress.Builder.newInstance().properties(properties).build();

    EdcSftpException edcSftpException =
        Assertions.assertThrows(
            EdcSftpException.class, () -> SftpDataAddress.fromDataAddress(dataAddress));
    Assertions.assertEquals("host is marked non-null but is null", edcSftpException.getMessage());
  }

  @Test
  void fromDataAddress__notSftp() {
    final Map<String, String> properties =
        Map.of(
            "type", "somethingOtherThanSftp",
            "locationHost", "localhost",
            "locationPort", "22",
            "locationPath", "path",
            "userName", "name",
            "userPassword", "password");

    final DataAddress dataAddress =
        DataAddress.Builder.newInstance().properties(properties).build();

    EdcSftpException edcSftpException =
        Assertions.assertThrows(
            EdcSftpException.class, () -> SftpDataAddress.fromDataAddress(dataAddress));
    Assertions.assertEquals(
        "Invalid DataAddress type: somethingOtherThanSftp. Expected sftp.",
        edcSftpException.getMessage());
  }
}
