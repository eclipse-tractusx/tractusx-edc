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

import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.TimeUnit;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.tractusx.edc.cp.adapter.dto.DataReferenceRetrievalDto;
import org.eclipse.tractusx.edc.cp.adapter.dto.ProcessData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ResultServiceTest {
  @Mock Monitor monitor;

  @BeforeEach
  void init() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void pull_shouldReturnDataReferenceWhenMessageOccursFirst() throws InterruptedException {
    // given
    ResultService resultService = new ResultService(20, monitor);
    String endpointDataRefId = "456";
    DataReferenceRetrievalDto dto = getDto(endpointDataRefId);
    ProcessData processData;

    // when
    resultService.process(dto);
    processData = resultService.pull(dto.getTraceId(), 200, TimeUnit.MILLISECONDS);

    // then
    Assertions.assertEquals(endpointDataRefId, processData.getEndpointDataReference().getId());
  }

  @Test
  public void pull_shouldReturnDataReferenceWhenMessageOccursSecond() throws InterruptedException {
    // given
    ResultService resultService = new ResultService(20, monitor);
    String endpointDataRefId = "456";
    DataReferenceRetrievalDto dto = getDto(endpointDataRefId);
    ProcessData processData;

    // when
    processMessageWithDelay(resultService, dto);
    processData = resultService.pull(dto.getTraceId(), 1000, TimeUnit.MILLISECONDS);

    // then
    Assertions.assertEquals(endpointDataRefId, processData.getEndpointDataReference().getId());
  }

  @Test
  public void pull_shouldReturnNullOnTimeout() throws InterruptedException {
    // given
    ResultService resultService = new ResultService(20, monitor);

    // when
    ProcessData processData = resultService.pull("123", 500, TimeUnit.MILLISECONDS);

    // then
    Assertions.assertNull(processData);
  }

  @Test
  public void process_shouldThrowIllegalArgumentExceptionIfNoDataPayload() {
    // given
    ResultService resultService = new ResultService(20, monitor);
    DataReferenceRetrievalDto dto = new DataReferenceRetrievalDto(null, 3);

    // when then
    try {
      resultService.process(dto);
      fail("Method should throw IllegalArgumentException");
    } catch (IllegalArgumentException ignored) {
    }
  }

  private void processMessageWithDelay(ResultService resultService, DataReferenceRetrievalDto dto) {
    new Thread(
            () -> {
              sleep(400);
              resultService.process(dto);
            })
        .start();
  }

  private DataReferenceRetrievalDto getDto(String endpointDataRefId) {
    DataReferenceRetrievalDto dto = new DataReferenceRetrievalDto(getProcessData(), 3);
    dto.getPayload()
        .setEndpointDataReference(
            EndpointDataReference.Builder.newInstance()
                .id(endpointDataRefId)
                .endpoint("e")
                .authCode("c")
                .authKey("k")
                .build());
    return dto;
  }

  private void sleep(long milisec) {
    try {
      Thread.sleep(milisec);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private ProcessData getProcessData() {
    return ProcessData.builder().assetId("assetId").provider("provider").build();
  }
}
