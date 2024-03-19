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

package org.eclipse.tractusx.edc.data.encryption;

import org.eclipse.edc.connector.transfer.dataplane.spi.security.DataEncrypter;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.tractusx.edc.data.encryption.aes.AesEncryptor;

import java.util.Objects;

import static java.lang.String.format;

@Extension(value = "Registers a DataEncryptor")
public class TxEncryptorExtension implements ServiceExtension {
    public static final String NAME = "Data Encryption Extension";
    public static final String AES_ALGORITHM = "AES";

    @Setting(value = "Vault alias, under which the encryption key is stored in the vault", required = true)
    public static final String ENCRYPTION_KEY_ALIAS = "edc.data.encryption.keys.alias";

    @Setting(value = "Algorithm to be used", defaultValue = AES_ALGORITHM)
    public static final String ENCRYPTION_ALGORITHM = "edc.data.encryption.algorithm";
    @Setting(value = "DEPRECATED - caching keys is unsafe and is not used anymore. Will be ignored.")
    @Deprecated
    public static final String CACHING_ENABLED = "edc.data.encryption.caching.enabled";
    @Setting(value = "DEPRECATED - caching keys is unsafe and is not used anymore. Will be ignored.")
    @Deprecated
    public static final String CACHING_SECONDS = "edc.data.encryption.caching.seconds";
    @Inject
    private Vault vault;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public DataEncrypter createEncryptor(ServiceExtensionContext context) {
        var keyAlias = context.getSetting(ENCRYPTION_KEY_ALIAS, null);
        Objects.requireNonNull(keyAlias, ENCRYPTION_KEY_ALIAS + " property not found");
        var algorithm = context.getSetting(ENCRYPTION_ALGORITHM, AES_ALGORITHM);

        if (context.getSetting(CACHING_ENABLED, null) != null || context.getSetting(CACHING_SECONDS, null) != null) {
            context.getMonitor().warning(format("Caching the secret keys was deprecated because it is unsafe. " +
                    "This version will ignore the properties '%s' and '%s' and will NOT cache the keys", CACHING_ENABLED, CACHING_SECONDS));
        }

        if (algorithm.equalsIgnoreCase(AES_ALGORITHM)) {
            return new AesEncryptor(vault, keyAlias);
        }
        context.getMonitor().warning(format("Algorithm %s is not known, will use a NOOP encryptor!", algorithm));
        return new DataEncrypter() {
            @Override
            public String encrypt(String s) {
                return s;
            }

            @Override
            public String decrypt(String s) {
                return s;
            }
        };
    }
}
