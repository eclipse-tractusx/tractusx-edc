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

import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Test;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SftpDataAddressTest {

    @Test
    void fromDataAddress_password() {
        var properties = Map.of(
                "type", "sftp",
                "locationHost", "localhost",
                "locationPort", "22",
                "locationPath", "path",
                "userName", "name",
                "userPassword", "password");

        var dataAddress = DataAddress.Builder.newInstance().properties(properties).build();

        var sftpDataAddress = SftpDataAddress.fromDataAddress(dataAddress);

        assertEquals("localhost", sftpDataAddress.getSftpLocation().getHost());
        assertEquals(22, sftpDataAddress.getSftpLocation().getPort());
        assertEquals("path", sftpDataAddress.getSftpLocation().getPath());
        assertEquals("name", sftpDataAddress.getSftpUser().getName());
        assertEquals("password", sftpDataAddress.getSftpUser().getPassword());
        assertNull(sftpDataAddress.getSftpUser().getKeyPair());
    }

    @Test
    void fromDataAddress_keyPair() throws NoSuchAlgorithmException {
        var keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        var keyPair = keyPairGenerator.generateKeyPair();
        var privateKeyBytes = keyPair.getPrivate().getEncoded();

        var properties =
                Map.of(
                        "type", "sftp",
                        "locationHost", "localhost",
                        "locationPort", "22",
                        "locationPath", "path",
                        "userName", "name",
                        "userPrivateKey", Base64.getEncoder().encodeToString(privateKeyBytes));

        var dataAddress =
                DataAddress.Builder.newInstance().properties(properties).build();

        var sftpDataAddress = SftpDataAddress.fromDataAddress(dataAddress);

        assertEquals("localhost", sftpDataAddress.getSftpLocation().getHost());
        assertEquals(22, sftpDataAddress.getSftpLocation().getPort());
        assertEquals("path", sftpDataAddress.getSftpLocation().getPath());
        assertEquals("name", sftpDataAddress.getSftpUser().getName());
        assertArrayEquals(
                privateKeyBytes, sftpDataAddress.getSftpUser().getKeyPair().getPrivate().getEncoded());
        assertNull(sftpDataAddress.getSftpUser().getPassword());
    }

    @Test
    void fromDataAddress_noAuth() {
        var properties =
                Map.of(
                        "type", "sftp",
                        "locationHost", "localhost",
                        "locationPort", "22",
                        "locationPath", "path",
                        "userName", "name");

        var dataAddress =
                DataAddress.Builder.newInstance().properties(properties).build();

        var sftpDataAddress = SftpDataAddress.fromDataAddress(dataAddress);

        assertEquals("localhost", sftpDataAddress.getSftpLocation().getHost());
        assertEquals(22, sftpDataAddress.getSftpLocation().getPort());
        assertEquals("path", sftpDataAddress.getSftpLocation().getPath());
        assertEquals("name", sftpDataAddress.getSftpUser().getName());
        assertNull(sftpDataAddress.getSftpUser().getPassword());
    }

    @Test
    void fromDataAddress_invalidKeyPairBrokenBase64() {
        var properties =
                Map.of(
                        "type", "sftp",
                        "locationHost", "localhost",
                        "locationPort", "22",
                        "locationPath", "path",
                        "userName", "name",
                        "userPrivateKey", "clearlyNotAPrivateKey");

        var dataAddress =
                DataAddress.Builder.newInstance().properties(properties).build();

        var edcSftpException =
                assertThrows(
                        EdcSftpException.class, () -> SftpDataAddress.fromDataAddress(dataAddress));
        assertEquals(
                "Cannot decode base64 private key for user name", edcSftpException.getMessage());
    }

    @Test
    void fromDataAddress_invalidKeyPairButCorrectBase64() {
        var properties =
                Map.of(
                        "type", "sftp",
                        "locationHost", "localhost",
                        "locationPort", "22",
                        "locationPath", "path",
                        "userName", "name",
                        "userPrivateKey", "TWFueSBoYW5kcyBtYWtlIGxpZ2h0IHdvcmsu");

        var dataAddress =
                DataAddress.Builder.newInstance().properties(properties).build();

        var edcSftpException =
                assertThrows(
                        EdcSftpException.class, () -> SftpDataAddress.fromDataAddress(dataAddress));
        assertEquals(
                "Unable to parse provided keypair for Sftp user name", edcSftpException.getMessage());
    }

    @Test
    void fromDataAddress_portNaN() {
        var properties =
                Map.of(
                        "type", "sftp",
                        "locationHost", "localhost",
                        "locationPort", "notANumber",
                        "locationPath", "path",
                        "userName", "name",
                        "userPassword", "password");

        var dataAddress = DataAddress.Builder.newInstance().properties(properties).build();

        var edcSftpException =
                assertThrows(
                        EdcSftpException.class, () -> SftpDataAddress.fromDataAddress(dataAddress));
        assertEquals(
                "Port for SftpLocation localhost/path not a number", edcSftpException.getMessage());
    }

    @Test
    void fromDataAddress_missingParameter() {
        var properties =
                Map.of(
                        "type", "sftp",
                        "locationPort", "22",
                        "locationPath", "path",
                        "userName", "name",
                        "userPassword", "password");

        var dataAddress = DataAddress.Builder.newInstance().properties(properties).build();

        assertThatThrownBy(() -> SftpDataAddress.fromDataAddress(dataAddress))
                .isInstanceOf(EdcSftpException.class)
                .hasMessageStartingWith("host");
    }

    @Test
    void fromDataAddress_notSftp() {
        var properties =
                Map.of(
                        "type", "somethingOtherThanSftp",
                        "locationHost", "localhost",
                        "locationPort", "22",
                        "locationPath", "path",
                        "userName", "name",
                        "userPassword", "password");

        var dataAddress = DataAddress.Builder.newInstance().properties(properties).build();

        var edcSftpException = assertThrows(EdcSftpException.class, () -> SftpDataAddress.fromDataAddress(dataAddress));
        assertEquals("Invalid DataAddress type: somethingOtherThanSftp. Expected sftp.",
                edcSftpException.getMessage());
    }
}
