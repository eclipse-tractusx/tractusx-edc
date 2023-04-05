/*
 * Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.eclipse.tractusx.edc.data.encryption.algorithms.aes;

import org.bouncycastle.util.encoders.Base64;
import org.eclipse.tractusx.edc.data.encryption.data.CryptoDataFactory;
import org.eclipse.tractusx.edc.data.encryption.data.CryptoDataFactoryImpl;
import org.eclipse.tractusx.edc.data.encryption.data.DecryptedData;
import org.eclipse.tractusx.edc.data.encryption.data.EncryptedData;
import org.eclipse.tractusx.edc.data.encryption.key.AesKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AesAlgorithmTest {

    private static final byte[] KEY_128_BIT = Base64.decode("dVUjmYJzbwVcntkFZU+lNQ==");
    private static final byte[] KEY_196_BIT = Base64.decode("NcgHzzRTUC+z396tWG9hqIbeihujz0m8");
    private static final byte[] KEY_256_BIT =
            Base64.decode("OSD+3NcZAmS/6UXbq6NL8UL+aQIAJDLL7BE2rBX5MtA=");

    private final AesAlgorithm strategy = new AesAlgorithm(new CryptoDataFactoryImpl());
    private final CryptoDataFactory cryptoDataFactory = new CryptoDataFactoryImpl();

    @Test
    void test128BitKey() {
        testKey(KEY_128_BIT);
    }

    @Test
    void test196BitKey() {
        testKey(KEY_196_BIT);
    }

    @Test
    void test256BitKey() {
        testKey(KEY_256_BIT);
    }

    @Test
    void testSameDataEncryptedDifferently() {
        final AesKey aesKey = createKey(KEY_128_BIT);
        final DecryptedData expected = cryptoDataFactory.decryptedFromText("same data");

        try {
            final EncryptedData result1 = strategy.encrypt(expected, aesKey);
            final EncryptedData result2 = strategy.encrypt(expected, aesKey);

            Assertions.assertNotEquals(result1.getBase64(), result2.getBase64());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    void testKey(byte[] key) {
        final AesKey aesKey = createKey(key);
        final DecryptedData expected = cryptoDataFactory.decryptedFromText("I will be encrypted");
        try {
            final EncryptedData encryptedResult = strategy.encrypt(expected, aesKey);
            final DecryptedData result = strategy.decrypt(encryptedResult, aesKey);
            Assertions.assertEquals(expected.getBase64(), result.getBase64());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    AesKey createKey(byte[] key) {
        return new AesKey() {

            @Override
            public byte[] getBytes() {
                return key;
            }

            @Override
            public String getBase64() {
                return Base64.toBase64String(key);
            }
        };
    }
}
