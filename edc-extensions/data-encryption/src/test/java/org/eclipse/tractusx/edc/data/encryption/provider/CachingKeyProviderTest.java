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
package org.eclipse.tractusx.edc.data.encryption.provider;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;
import org.eclipse.tractusx.edc.data.encryption.key.CryptoKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@SuppressWarnings("FieldCanBeLocal")
class CachingKeyProviderTest {

  private CachingKeyProvider<CryptoKey> cachingKeyProvider;

  private CryptoKey encryptionKey;
  private CryptoKey decryptionKey;

  // mocks
  private KeyProvider<CryptoKey> decoratedProvider;
  private Duration cacheExpiration;
  private Clock clock;

  @BeforeEach
  void setup() {
    decoratedProvider = Mockito.mock(KeyProvider.class);
    cacheExpiration = Duration.ofSeconds(2);
    clock = Mockito.mock(Clock.class);
    encryptionKey = Mockito.mock(CryptoKey.class);
    decryptionKey = Mockito.mock(CryptoKey.class);

    cachingKeyProvider =
        new CachingKeyProvider<CryptoKey>(decoratedProvider, cacheExpiration, clock);

    Mockito.when(decoratedProvider.getEncryptionKey()).thenReturn(encryptionKey);
    Mockito.when(decoratedProvider.getDecryptionKeySet())
        .thenAnswer((i) -> Stream.of(decryptionKey));
  }

  @Test
  void testCaching() {

    Mockito.when(clock.instant()).thenAnswer((i) -> Instant.now());

    cachingKeyProvider.getDecryptionKeySet();
    cachingKeyProvider.getEncryptionKey();

    cachingKeyProvider.getDecryptionKeySet();
    cachingKeyProvider.getEncryptionKey();

    cachingKeyProvider.getDecryptionKeySet();
    cachingKeyProvider.getEncryptionKey();

    cachingKeyProvider.getDecryptionKeySet();
    cachingKeyProvider.getEncryptionKey();

    Mockito.verify(decoratedProvider, Mockito.times(1)).getDecryptionKeySet();
    Mockito.verify(decoratedProvider, Mockito.times(1)).getEncryptionKey();
  }

  @Test
  void testCacheUpdate() {

    Mockito.when(clock.instant()).thenAnswer((i) -> Instant.now());

    cachingKeyProvider.getDecryptionKeySet();
    cachingKeyProvider.getEncryptionKey();

    cachingKeyProvider.getDecryptionKeySet();
    cachingKeyProvider.getEncryptionKey();

    Mockito.when(clock.instant())
        .thenAnswer((i) -> Instant.now().plus(cacheExpiration.plusSeconds(1)));

    cachingKeyProvider.getDecryptionKeySet();
    cachingKeyProvider.getEncryptionKey();

    Mockito.verify(decoratedProvider, Mockito.times(2)).getDecryptionKeySet();
    Mockito.verify(decoratedProvider, Mockito.times(2)).getEncryptionKey();
  }
}
