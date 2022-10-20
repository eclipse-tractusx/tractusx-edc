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

package net.catenax.edc.cp.adapter.service;

import static java.util.Objects.isNull;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import net.catenax.edc.cp.adapter.dto.DataReferenceRetrievalDto;
import net.catenax.edc.cp.adapter.dto.ProcessData;
import net.catenax.edc.cp.adapter.messaging.Listener;

@RequiredArgsConstructor
public class ResultService implements Listener<DataReferenceRetrievalDto> {
  private final int CAPACITY = 1;
  private final int DEFAULT_TIMEOUT;
  private final Map<String, ArrayBlockingQueue<ProcessData>> results = new ConcurrentHashMap<>();

  public ProcessData pull(String id) throws InterruptedException {
    return pull(id, DEFAULT_TIMEOUT, SECONDS);
  }

  public ProcessData pull(String id, long timeout, TimeUnit unit) throws InterruptedException {
    if (!results.containsKey(id)) {
      initiate(id);
    }
    ProcessData result = results.get(id).poll(timeout, unit);
    results.remove(id);
    return result;
  }

  @Override
  public void process(DataReferenceRetrievalDto dto) {
    if (isNull(dto) || isNull(dto.getPayload())) {
      throw new IllegalArgumentException();
    }
    add(dto.getTraceId(), dto.getPayload());
  }

  private void add(String id, ProcessData ProcessData) {
    if (!results.containsKey(id)) {
      initiate(id);
    }
    results.get(id).add(ProcessData);
  }

  private void initiate(String id) {
    results.put(id, new ArrayBlockingQueue<>(CAPACITY));
  }
}
