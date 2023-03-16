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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ObjectStoreServiceInMemory implements ObjectStoreService {
  private final ObjectMapper mapper;
  private final Map<String, String> map = new HashMap<>();

  @Override
  public void put(String key, ObjectType objectType, Object object) {
    try {
      map.put(getKey(key, objectType), mapper.writeValueAsString(object));
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      throw new IllegalArgumentException();
    }
  }

  @Override
  public <T> T get(String key, ObjectType objectType, Class<T> type) {
    String json = map.get(getKey(key, objectType));
    return Objects.isNull(json) ? null : map(type, json);
  }

  @Override
  public <T> List<T> get(ObjectType objectType, Class<T> type) {
    return map.entrySet().stream()
        .filter(entry -> entry.getKey().startsWith(objectType.name()))
        .map(Map.Entry::getValue)
        .map(s -> map(type, s))
        .collect(Collectors.toList());
  }

  @Override
  public void remove(String key, ObjectType objectType) {
    map.remove(getKey(key, objectType));
  }

  private String getKey(String key, ObjectType objectType) {
    return objectType.name() + key;
  }

  private <T> T map(Class<T> type, String json) {
    T object = null;
    try {
      object = mapper.readValue(json, type);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    return object;
  }
}
