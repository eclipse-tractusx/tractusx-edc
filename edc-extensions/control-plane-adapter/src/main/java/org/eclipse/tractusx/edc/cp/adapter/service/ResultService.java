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

package org.eclipse.tractusx.edc.cp.adapter.service;

import static java.util.Objects.isNull;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.tractusx.edc.cp.adapter.dto.DataReferenceRetrievalDto;
import org.eclipse.tractusx.edc.cp.adapter.dto.ProcessData;
import org.eclipse.tractusx.edc.cp.adapter.messaging.Listener;

@RequiredArgsConstructor
public class ResultService implements Listener<DataReferenceRetrievalDto> {
  private final int CAPACITY = 1;
  private final int DEFAULT_TIMEOUT;
  private final Map<String, ArrayBlockingQueue<ProcessData>> results = new ConcurrentHashMap<>();
  private final Monitor monitor;

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
    logReceivedResult(dto);
    add(dto.getTraceId(), dto.getPayload());
  }

  private void add(String id, ProcessData processData) {
    if (!results.containsKey(id)) {
      initiate(id);
    }
    try {
      results.get(id).add(processData);
    } catch (IllegalStateException e) {
      logIgnoredResult(id, processData);
    }
  }

  private void initiate(String id) {
    results.put(id, new ArrayBlockingQueue<>(CAPACITY));
  }

  private void logReceivedResult(DataReferenceRetrievalDto dto) {
    monitor.info(
        String.format(
            "[%s] Result received: %s", dto.getTraceId(), getResultInfo(dto.getPayload())));
  }

  private void logIgnoredResult(String id, ProcessData processData) {
    monitor.warning(
        String.format(
            "[%s] Other Result was already returned! Result '%s' will be ignored!",
            id, getResultInfo(processData)));
  }

  private String getResultInfo(ProcessData processData) {
    return Objects.nonNull(processData.getErrorMessage())
        ? processData.getErrorMessage()
        : processData.getEndpointDataReference().getId();
  }
}
