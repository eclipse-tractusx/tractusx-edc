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

package net.catenax.edc.cp.adapter.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class LockMap {
  private final Map<String, ReentrantLock> lock = new HashMap<>();

  public void lock(String id) {
    addLock(id);
    lock.get(id).lock();
  }

  public void unlock(String id) {
    addLock(id);
    lock.get(id).unlock();
  }

  public void removeLock(String id) {
    addLock(id);
    lock.remove(id);
  }

  private void addLock(String id) {
    synchronized (this) {
      lock.putIfAbsent(id, new ReentrantLock());
    }
  }
}
