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

package net.catenax.edc.cp.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.Response;
import net.catenax.edc.cp.adapter.messaging.MessageBus;
import net.catenax.edc.cp.adapter.service.ResultService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
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
    Response response = httpController.getAssetSynchronous(null, "providerUrl");

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
    Response response = httpController.getAssetSynchronous("assetId", null);

    // then
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
}
