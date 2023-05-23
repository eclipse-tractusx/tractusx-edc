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

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Objects;

public class SftpUserKeyPairGenerator {
    public static KeyPair getKeyPairFromPrivateKey(byte[] privateKeyBytes, String sftpUserName) {
        Objects.requireNonNull(sftpUserName, "sftpUsername");
        if (privateKeyBytes == null) {
            return null;
        }

        try {
            var keyFactory = KeyFactory.getInstance("RSA");
            var privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));

            var privateKeySpec = (RSAPrivateCrtKey) privateKey;

            var publicKey = keyFactory.generatePublic(new RSAPublicKeySpec(privateKeySpec.getModulus(), privateKeySpec.getPublicExponent()));
            return new KeyPair(publicKey, privateKey);

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new EdcSftpException(String.format("Unable to parse provided keypair for Sftp user %s", sftpUserName), e);
        }
    }

    public static KeyPair getKeyPairFromPrivateKey(String privateKeyBase64, String sftpUserName) {
        if (privateKeyBase64 == null) {
            return null;
        }

        try {
            var publicBytes = Base64.getDecoder().decode(privateKeyBase64);
            return getKeyPairFromPrivateKey(publicBytes, sftpUserName);
        } catch (IllegalArgumentException e) {
            throw new EdcSftpException(String.format("Cannot decode base64 private key for user %s", sftpUserName), e);
        }

    }
}
