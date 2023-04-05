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
package org.eclipse.tractusx.edc.data.encryption.data;

import org.bouncycastle.util.encoders.Base64;

public class CryptoDataFactoryImpl implements CryptoDataFactory {

    public DecryptedData decryptedFromText(String text) {
        final byte[] bytes = text.getBytes();
        final String base64 = Base64.toBase64String(bytes);
        return new DecryptedDataImpl(bytes, base64, text);
    }

    public DecryptedData decryptedFromBase64(String base64) {
        final byte[] bytes = Base64.decode(base64);
        final String text = new String(bytes);
        return new DecryptedDataImpl(bytes, base64, text);
    }

    public DecryptedData decryptedFromBytes(byte[] bytes) {
        final String base64 = Base64.toBase64String(bytes);
        final String text = new String(bytes);
        return new DecryptedDataImpl(bytes, base64, text);
    }

    public EncryptedData encryptedFromText(String text) {
        final byte[] bytes = text.getBytes();
        final String base64 = Base64.toBase64String(bytes);
        return new EncryptedDataImpl(bytes, base64, text);
    }

    public EncryptedData encryptedFromBase64(String base64) {
        final byte[] bytes = Base64.decode(base64);
        final String text = new String(bytes);
        return new EncryptedDataImpl(bytes, base64, text);
    }

    public EncryptedData encryptedFromBytes(byte[] bytes) {
        final String base64 = Base64.toBase64String(bytes);
        final String text = new String(bytes);
        return new EncryptedDataImpl(bytes, base64, text);
    }


    private static class DecryptedDataImpl implements DecryptedData {
        private final byte[] bytes;
        private final String base64;
        private final String text;

        private DecryptedDataImpl(byte[] bytes, String base64, String text) {
            this.bytes = bytes;
            this.base64 = base64;
            this.text = text;
        }

        @Override
        public byte[] getBytes() {
            return bytes;
        }

        @Override
        public String getBase64() {
            return base64;
        }
    }


    private static class EncryptedDataImpl implements EncryptedData {
        private final byte[] bytes;
        private final String base64;
        private final String text;

        private EncryptedDataImpl(byte[] bytes, String base64, String text) {
            this.bytes = bytes;
            this.base64 = base64;
            this.text = text;
        }

        @Override
        public byte[] getBytes() {
            return bytes;
        }

        @Override
        public String getBase64() {
            return base64;
        }
    }
}
