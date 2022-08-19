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

package net.catenax.edc.data.encryption.provider;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.Value;
import net.catenax.edc.data.encryption.key.CryptoKey;

public class CachingKeyProvider<T extends CryptoKey> implements KeyProvider<T> {

  @NonNull private final KeyProvider<T> decoratedProvider;
  @NonNull private final Clock clock;
  @NonNull private final Duration cacheExpiration;

  private CachedKeys<T> cachedKeys;

  public CachingKeyProvider(KeyProvider<T> keyProvider, Duration cacheExpiration) {
    this(keyProvider, cacheExpiration, Clock.systemUTC());
  }

  public CachingKeyProvider(KeyProvider<T> keyProvider, Duration cacheExpiration, Clock clock) {

    this.decoratedProvider = keyProvider;
    this.cacheExpiration = cacheExpiration;
    this.clock = clock;
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

  @Value
  private static class CachedKeys<T> {
    T encryptionKey;
    List<T> decryptionKeys;
    @NonNull Instant expiration;
  }
}
