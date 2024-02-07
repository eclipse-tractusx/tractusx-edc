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

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.security.Vault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.BadPaddingException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AesEncryptorTest {

    private static final String TESTALIAS = "test-alias";
    private final Vault vaultMock = mock();
    private AesEncryptor encryptor;

    @BeforeEach
    void setup() {
        encryptor = new AesEncryptor(vaultMock, TESTALIAS);
    }

    @ParameterizedTest
    @ValueSource(ints = {16, 24, 32})
    void encrypt(int validSize) {
        when(vaultMock.resolveSecret(eq(TESTALIAS))).thenReturn(generateBase64(validSize));

        var encrypted = encryptor.encrypt("foobar barbaz");
        assertThat(encrypted).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 17, 33, 1024, 8192})
    void encrypt_invalidKeySize(int invalidKeySize) {
        when(vaultMock.resolveSecret(eq(TESTALIAS))).thenReturn(generateBase64(invalidKeySize));
        assertThatThrownBy(() -> encryptor.encrypt("hello world!"))
                .isInstanceOf(EdcException.class)
                .hasMessage("Expected a key size of 16, 24 or 32 bytes byt found " + invalidKeySize);
    }

    @Test
    void encrypt_secretNotBase64() {
        when(vaultMock.resolveSecret(eq(TESTALIAS))).thenReturn("not-base64");

        assertThatThrownBy(() -> encryptor.encrypt("hello world!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Illegal base64 character");
    }

    @Test
    void encrypt_secretNotExist() {
        assertThatThrownBy(() -> encryptor.encrypt("hello world!"))
                .isInstanceOf(EdcException.class)
                .hasMessage("Cannot perform AES encryption: secret key not found in vault");
    }

    @Test
    void decrypt() {
        when(vaultMock.resolveSecret(eq(TESTALIAS))).thenReturn(generateBase64(32));
        var rawText = "hello world!";
        var encrypted = encryptor.encrypt(rawText);
        assertThat(encryptor.decrypt(encrypted)).isEqualTo(rawText);
    }

    @Test
    void decrypt_wrongSecretKey() {
        when(vaultMock.resolveSecret(eq(TESTALIAS))).thenReturn(generateBase64(32));
        var rawText = "hello world!";
        var encrypted = encryptor.encrypt(rawText);
        when(vaultMock.resolveSecret(eq(TESTALIAS))).thenReturn(generateBase64(32));
        assertThatThrownBy(() -> encryptor.decrypt(encrypted)).isInstanceOf(EdcException.class)
                .hasMessage("Error while decrypting")
                .hasRootCauseInstanceOf(BadPaddingException.class);
    }

    @Test
    void decrypt_keyNotBase64() {
        when(vaultMock.resolveSecret(eq(TESTALIAS))).thenReturn("not-base64");
        assertThatThrownBy(() -> encryptor.decrypt("encrypted-text"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Illegal base64 character");
    }

    @Test
    void decrypt_ciphertextTooShort() {
        when(vaultMock.resolveSecret(eq(TESTALIAS))).thenReturn(generateBase64(32));
        var cipherText = Base64.getEncoder().encodeToString("asdf".getBytes());
        assertThatThrownBy(() -> encryptor.decrypt(cipherText))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Decoded ciphertext was shorter than the IV size (16)");
    }

    private String generateBase64(int validSize) {
        var bytes = new byte[validSize];

        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);

    }
}
