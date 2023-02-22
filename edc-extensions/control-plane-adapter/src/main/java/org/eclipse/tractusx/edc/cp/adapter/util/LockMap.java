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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * When CP-Adapter works in InMemory mode, LockMap is used to prevent race condition of two events.
 * This implementation will not work if both events will be handled by two separate EDC instances
 * (persistent mode), but edc.cp.adapter.service.objectstore.ObjectStoreServiceSql#put(...) method
 * will not allow to save both events in the table as PRIMARY_KE collision would appear.
 */
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
