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
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
public class SftpUserKeyPairGenerator {
  public static KeyPair getKeyPairFromPrivateKey(
      byte[] privateKeyBytes, @NonNull String sftpUserName) {
    if (privateKeyBytes == null) {
      return null;
    }

    try {
      final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      final PrivateKey privateKey =
          keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));

      final RSAPrivateCrtKey privateKeySpec = (RSAPrivateCrtKey) privateKey;

      final PublicKey publicKey =
          keyFactory.generatePublic(
              new RSAPublicKeySpec(
                  privateKeySpec.getModulus(), privateKeySpec.getPublicExponent()));
      return new KeyPair(publicKey, privateKey);

    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new EdcSftpException(
          String.format("Unable to parse provided keypair for Sftp user %s", sftpUserName), e);
    }
  }

  public static KeyPair getKeyPairFromPrivateKey(String privateKey, @NonNull String sftpUserName) {
    if (privateKey == null) {
      return null;
    }

    byte[] publicBytes;
    try {
      publicBytes = Base64.getDecoder().decode(privateKey);
    } catch (IllegalArgumentException e) {
      throw new EdcSftpException(
          String.format("Cannot decode base64 private key for user %s", sftpUserName), e);
    }

    return getKeyPairFromPrivateKey(publicBytes, sftpUserName);
  }
}
