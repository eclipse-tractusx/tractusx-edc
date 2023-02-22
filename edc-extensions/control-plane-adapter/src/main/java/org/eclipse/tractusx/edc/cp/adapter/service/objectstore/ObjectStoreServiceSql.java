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
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.eclipse.edc.util.collection.CollectionUtil;
import org.eclipse.tractusx.edc.cp.adapter.store.SqlObjectStore;
import org.eclipse.tractusx.edc.cp.adapter.store.model.ObjectEntity;

@AllArgsConstructor
public class ObjectStoreServiceSql implements ObjectStoreService {
  private final ObjectMapper mapper;
  private final SqlObjectStore objectStore;

  @Override
  public void put(String key, ObjectType objectType, Object object) {
    ObjectEntity entity =
        ObjectEntity.builder()
            .id(key)
            .type(objectType.name())
            .object(objectToJson(object, objectType.name()))
            .build();
    objectStore.saveMessage(entity);
  }

  @Override
  public <T> T get(String key, ObjectType objectType, Class<T> type) {
    ObjectEntity entity = objectStore.find(key, objectType.name());
    if (Objects.isNull(entity)) {
      return null;
    }
    return jsonToObject(entity, type);
  }

  @Override
  public <T> List<T> get(ObjectType objectType, Class<T> type) {
    List<ObjectEntity> entities = objectStore.find(objectType.name());
    if (CollectionUtil.isEmpty(entities)) {
      return List.of();
    }
    return entities.stream().map(entity -> jsonToObject(entity, type)).collect(Collectors.toList());
  }

  @Override
  public void remove(String key, ObjectType objectType) {
    objectStore.deleteMessage(key, objectType.name());
  }

  private String objectToJson(Object object, String type) {
    if (Objects.isNull(object)) {
      return null;
    }
    try {
      return mapper.writeValueAsString(object);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      throw new IllegalArgumentException(String.format("Can not parse object of type %s", type));
    }
  }

  private <T> T jsonToObject(ObjectEntity entity, Class<T> type) {
    try {
      return mapper.readValue(entity.getObject(), type);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      throw new IllegalArgumentException(String.format("Can not parse object of type %s", type));
    }
  }
}
