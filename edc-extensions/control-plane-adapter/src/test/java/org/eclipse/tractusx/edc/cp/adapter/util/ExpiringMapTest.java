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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ExpiringMapTest {

  private static final String KEY = "key";
  private static final String VAL = "value";

  @Test
  public void get_shouldGetWhenNotExpired() {
    // given
    ExpiringMap<String, String> expiringMap = new ExpiringMap<>(60);
    expiringMap.put(KEY, VAL);

    // when
    String value = expiringMap.get(KEY);

    // then
    Assertions.assertEquals(VAL, value);
  }

  @Test
  public void get_shouldGetNullWhenExpired() throws InterruptedException {
    // given
    ExpiringMap<String, String> expiringMap = new ExpiringMap<>(0);
    expiringMap.put(KEY, VAL);

    // when
    Thread.sleep(1000);
    String value = expiringMap.get(KEY, 0);

    // then
    Assertions.assertNull(value);
  }

  @Test
  public void get_shouldGetNullWhenRemoved() throws InterruptedException {
    // given
    ExpiringMap<String, String> expiringMap = new ExpiringMap<>(0);
    expiringMap.put(KEY, VAL);

    // when
    expiringMap.remove(KEY);
    String value = expiringMap.get(KEY, 1000);

    // then
    Assertions.assertNull(value);
  }
}
