/*
 * Copyright (c) 2022 ZF Friedrichshafen AG
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 * ZF Friedrichshafen AG - Initial API and Implementation
 *
 */

package org.eclipse.tractusx.edc.cp.adapter.util;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ExpiringMap<K, V> {
  private final Map<K, V> map = new HashMap<>();
  private final Map<K, Long> entryTimeMap = new HashMap<>();
  private long expireAfter = 2 * 60;

  public ExpiringMap() {}

  public ExpiringMap(long expireAfter) {
    this.expireAfter = expireAfter;
  }

  public void put(K key, V value) {
    map.put(key, value);
    entryTimeMap.put(key, now());
  }

  public V get(K key) {
    return get(key, expireAfter);
  }

  public V get(K key, long expireAfter) {
    V value = map.get(key);

    if (Objects.isNull(value)) {
      return null;
    }

    Long entryTime = entryTimeMap.get(key);
    if (Objects.isNull(entryTime)) {
      map.remove(key);
      return null;
    }

    if (entryTime + expireAfter < now()) {
      return null;
    }

    return value;
  }

  public void remove(K key) {
    map.remove(key);
    entryTimeMap.remove(key);
  }

  private long now() {
    return Instant.now().getEpochSecond();
  }
}
