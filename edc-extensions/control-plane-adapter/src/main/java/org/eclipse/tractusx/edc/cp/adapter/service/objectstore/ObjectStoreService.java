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

package org.eclipse.tractusx.edc.cp.adapter.service.objectstore;

import java.util.List;

public interface ObjectStoreService {
  void put(String key, ObjectType objectType, Object object);

  <T> T get(String key, ObjectType objectType, Class<T> type);

  void remove(String key, ObjectType objectType);

  <T> List<T> get(ObjectType objectType, Class<T> type);
}
