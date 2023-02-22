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

package org.eclipse.tractusx.edc.cp.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.Response;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.tractusx.edc.cp.adapter.dto.ProcessData;
import org.eclipse.tractusx.edc.cp.adapter.messaging.MessageBus;
import org.eclipse.tractusx.edc.cp.adapter.service.ResultService;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

public class HttpControllerTest {
  @Mock ApiAdapterConfig config = Mockito.mock(ApiAdapterConfig.class);
  Integer RETRY_NUMBER = 3;

  @Test
  public void getAssetSynchronous_shouldReturnBadRequestIfNoAssetIdParam() {
    // given
    Monitor monitor = Mockito.mock(Monitor.class);
    ResultService resultService = Mockito.mock(ResultService.class);
    MessageBus messageBus = Mockito.mock(MessageBus.class);
    when(config.getDefaultMessageRetryNumber()).thenReturn(RETRY_NUMBER);
    HttpController httpController = new HttpController(monitor, resultService, messageBus, config);

    // when
    Response response = httpController.getAssetSynchronous(null, "providerUrl", null, false, null);

    // then
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  public void getAssetSynchronous_shouldReturnBadRequestIfNoProviderUrlParam() {
    // given
    Monitor monitor = Mockito.mock(Monitor.class);
    ResultService resultService = Mockito.mock(ResultService.class);
    MessageBus messageBus = Mockito.mock(MessageBus.class);
    ApiAdapterConfig config = Mockito.mock(ApiAdapterConfig.class);
    when(config.getDefaultMessageRetryNumber()).thenReturn(RETRY_NUMBER);
    HttpController httpController = new HttpController(monitor, resultService, messageBus, config);

    // when
    Response response = httpController.getAssetSynchronous("assetId", null, null, false, null);

    // then
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  public void getAssetSynchronous_shouldReturnErrorStatusIfOccurred() throws InterruptedException {
    // given
    Monitor monitor = Mockito.mock(Monitor.class);
    ResultService resultService = Mockito.mock(ResultService.class);
    MessageBus messageBus = Mockito.mock(MessageBus.class);
    ApiAdapterConfig config = Mockito.mock(ApiAdapterConfig.class);
    when(config.getDefaultMessageRetryNumber()).thenReturn(RETRY_NUMBER);
    HttpController httpController = new HttpController(monitor, resultService, messageBus, config);

    when(resultService.pull(anyString()))
        .thenReturn(
            ProcessData.builder()
                .errorStatus(Response.Status.BAD_GATEWAY)
                .endpointDataReference(getEndpointDataReference())
                .build());

    // when
    Response response =
        httpController.getAssetSynchronous("assetId", "providerUrl", null, false, null);

    // then
    assertEquals(Response.Status.BAD_GATEWAY.getStatusCode(), response.getStatus());
  }

  @Test
  public void getAssetSynchronous_shouldReturnOkResponse() throws InterruptedException {
    // given
    Monitor monitor = Mockito.mock(Monitor.class);
    ResultService resultService = Mockito.mock(ResultService.class);
    MessageBus messageBus = Mockito.mock(MessageBus.class);
    ApiAdapterConfig config = Mockito.mock(ApiAdapterConfig.class);
    when(config.getDefaultMessageRetryNumber()).thenReturn(RETRY_NUMBER);
    HttpController httpController = new HttpController(monitor, resultService, messageBus, config);
    when(resultService.pull(anyString()))
        .thenReturn(
            ProcessData.builder().endpointDataReference(getEndpointDataReference()).build());

    // when
    Response response =
        httpController.getAssetSynchronous("assetId", "providerUrl", null, false, null);

    // then
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
  }

  private EndpointDataReference getEndpointDataReference() {
    return EndpointDataReference.Builder.newInstance()
        .endpoint("endpoint")
        .authCode("authCode")
        .authKey("authKey")
        .build();
  }
}
