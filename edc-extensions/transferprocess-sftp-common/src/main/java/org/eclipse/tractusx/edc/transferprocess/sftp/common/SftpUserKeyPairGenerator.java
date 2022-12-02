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
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SftpUserKeyPairGenerator {
  public static KeyPair getKeyPairFromPrivateKey(byte[] privateKeyBytes, String sftpUserName) {
    if (privateKeyBytes == null) {
      return null;
    }

    try {
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));

      RSAPrivateCrtKey privateKeySpec = (RSAPrivateCrtKey) privateKey;

      PublicKey publicKey =
          keyFactory.generatePublic(
              new RSAPublicKeySpec(
                  privateKeySpec.getModulus(), privateKeySpec.getPublicExponent()));
      return new KeyPair(publicKey, privateKey);

    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new EdcSftpException(
          String.format("Unable to parse provided keypair for Sftp user %s", sftpUserName), e);
    }
  }
}
