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

package org.eclipse.tractusx.edc.data.encryption.provider;

import org.eclipse.tractusx.edc.data.encryption.key.CryptoKey;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CachingKeyProvider<T extends CryptoKey> implements KeyProvider<T> {

    private final KeyProvider<T> decoratedProvider;
    private final Clock clock;
    private final Duration cacheExpiration;

    private CachedKeys<T> cachedKeys;

    public CachingKeyProvider(KeyProvider<T> keyProvider, Duration cacheExpiration) {
        this(keyProvider, cacheExpiration, Clock.systemUTC());
    }

    public CachingKeyProvider(KeyProvider<T> keyProvider, Duration cacheExpiration, Clock clock) {
        this.decoratedProvider = Objects.requireNonNull(keyProvider);
        this.cacheExpiration = Objects.requireNonNull(cacheExpiration);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public T getEncryptionKey() {
        checkCache();
        return cachedKeys.getEncryptionKey();
    }

    @Override
    public Stream<T> getDecryptionKeySet() {
        checkCache();
        return cachedKeys.getDecryptionKeys().stream();
    }

    private void checkCache() {
        if (cachedKeys == null || cachedKeys.expiration.isBefore(clock.instant())) {
            T encryptionKey = decoratedProvider.getEncryptionKey();
            List<T> decryptionKeys = decoratedProvider.getDecryptionKeySet().collect(Collectors.toList());
            cachedKeys =
                    new CachedKeys<>(encryptionKey, decryptionKeys, clock.instant().plus(cacheExpiration));
        }
    }


    private static class CachedKeys<T> {
        private final T encryptionKey;
        private final List<T> decryptionKeys;
        private final Instant expiration;

        private CachedKeys(T encryptionKey, List<T> decryptionKeys, Instant expiration) {
            this.encryptionKey = encryptionKey;
            this.decryptionKeys = decryptionKeys;
            this.expiration = Objects.requireNonNull(expiration);
        }

        public List<T> getDecryptionKeys() {
            return decryptionKeys;
        }

        public T getEncryptionKey() {
            return encryptionKey;
        }
    }
}
