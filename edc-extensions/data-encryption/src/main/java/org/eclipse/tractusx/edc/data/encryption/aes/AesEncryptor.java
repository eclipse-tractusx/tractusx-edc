/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.tractusx.edc.data.encryption.aes;

import org.eclipse.edc.connector.transfer.dataplane.spi.security.DataEncrypter;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.tractusx.edc.data.encryption.ArrayUtil;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static org.eclipse.tractusx.edc.data.encryption.ArrayUtil.concat;

/**
 * This class implements the {@link DataEncrypter} interface by encrypting and decrypting an input string using the <a href="https://nvlpubs.nist.gov/nistpubs/legacy/sp/nistspecialpublication800-38d.pdf">GCM mode</a>.
 * Furthermore, it uses the {@code AES/CBC/PKCS5Padding} alongside the {@link java.security.SecureRandom} class to generate initialisation vectors (IV).
 */
public class AesEncryptor implements DataEncrypter {

    public static final int GCM_AUTH_TAG_LENGTH = 128;
    public static final String AES = "AES";
    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int IV_SIZE_BYTES = 16; // 16 bytes
    private static final int[] ALLOWED_SIZES = new int[] {16, 24, 32}; // AES allows for 128, 192 or 256 bits
    private final Vault vault;
    private final String secretAlias;
    private final Base64.Decoder decoder = Base64.getDecoder();
    private final Base64.Encoder encoder = Base64.getEncoder();
    private final Cipher cipher;

    /**
     * Initializes the encryptor with private key (stored in the vault). The key, that was stored in the vault, is expected to be in Base64 format, and
     * this encryptor will produce encrypted text in Base64.
     *
     * @param vault       A Vault instance that contains the encryption key. Keys should never be held in memory.
     * @param secretAlias The alias under which the key was stored in the vault. The key, which is stored in the vault under the given alias, must be in Base64 format
     */
    public AesEncryptor(Vault vault, String secretAlias) {
        this.vault = vault;
        this.secretAlias = secretAlias;
        try {
            cipher = Cipher.getInstance(AES_GCM);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new EdcException("Error while instantiating Cipher", e);
        }
    }


    @Override
    public String encrypt(String raw) {
        var key = getKey(secretAlias);
        var iv = generateIv(IV_SIZE_BYTES);
        try {
            var gcmSpec = new GCMParameterSpec(GCM_AUTH_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);

            byte[] cipherText = cipher.doFinal(raw.getBytes());
            var ivAndCipher = concat(iv, cipherText);

            return encoder.encodeToString(ivAndCipher);
        } catch (InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {
            throw new EdcException("Error while encrypting", e);
        }
    }

    /**
     * The encrypted text is expected in Base64 format: it is simply the ciphertext encoded as Base64
     */
    @Override
    public String decrypt(String encrypted) {
        var key = getKey(secretAlias);
        // will throw IllegalArgumentException if not base64
        var decodedCipher = decoder.decode(encrypted);

        if (decodedCipher.length < IV_SIZE_BYTES) {
            throw new IllegalArgumentException("Decoded ciphertext was shorter than the IV size (" + IV_SIZE_BYTES + ")");
        }
        var iv = ArrayUtil.subArray(decodedCipher, 0, IV_SIZE_BYTES);
        var cipherText = ArrayUtil.subArray(decodedCipher, IV_SIZE_BYTES, decodedCipher.length - IV_SIZE_BYTES);

        var gcmSpec = new GCMParameterSpec(GCM_AUTH_TAG_LENGTH, iv);

        try {
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);
            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText);
        } catch (InvalidAlgorithmParameterException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
            throw new EdcException("Error while decrypting", e);
        }
    }

    private byte[] generateIv(int length) {
        byte[] iv = new byte[length];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    private SecretKey getKey(String alias) {
        var secretBase64 = vault.resolveSecret(alias);
        if (secretBase64 == null) {
            throw new EdcException("Cannot perform AES encryption: secret key not found in vault");
        }
        var decoded = decoder.decode(secretBase64);
        if (isAllowedSize(decoded)) {
            return new SecretKeySpec(decoded, AES);
        }
        throw new EdcException("Expected a key size of 16, 24 or 32 bytes byt found " + decoded.length);
    }

    /**
     * Check is the decoded byte array is 16, 24 or 32 bytes in size
     */
    private boolean isAllowedSize(byte[] decoded) {
        return Arrays.stream(ALLOWED_SIZES).anyMatch(i -> i == decoded.length);
    }


}
