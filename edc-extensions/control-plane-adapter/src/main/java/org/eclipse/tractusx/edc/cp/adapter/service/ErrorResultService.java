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

import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.tractusx.edc.cp.adapter.dto.DataReferenceRetrievalDto;
import org.eclipse.tractusx.edc.cp.adapter.exception.ExternalRequestException;
import org.eclipse.tractusx.edc.cp.adapter.exception.ResourceNotFoundException;
import org.eclipse.tractusx.edc.cp.adapter.messaging.Channel;
import org.eclipse.tractusx.edc.cp.adapter.messaging.Listener;
import org.eclipse.tractusx.edc.cp.adapter.messaging.MessageBus;

@RequiredArgsConstructor
public class ErrorResultService implements Listener<DataReferenceRetrievalDto> {
  private static final Map<Class<?>, Response.Status> statusOfException = new HashMap<>();

  static {
    statusOfException.put(ExternalRequestException.class, Response.Status.BAD_GATEWAY);
    statusOfException.put(ResourceNotFoundException.class, Response.Status.NOT_FOUND);
  }

  private final Monitor monitor;
  private final MessageBus messageBus;

  @Override
  public void process(DataReferenceRetrievalDto dto) {
    dto.getPayload().setErrorMessage(getErrorMessage(dto));
    dto.getPayload()
        .setErrorStatus(
            statusOfException.getOrDefault(
                dto.getFinalException().getClass(), Response.Status.INTERNAL_SERVER_ERROR));
    log(dto);
    messageBus.send(Channel.RESULT, dto);
  }

  private String getErrorMessage(DataReferenceRetrievalDto dto) {
    return Objects.nonNull(dto.getFinalException())
        ? dto.getFinalException().getMessage()
        : "Unrecognized Exception.";
  }

  private void log(DataReferenceRetrievalDto dto) {
    monitor.info(
        String.format(
            "[%s] Sending ERROR message to RESULT channel: %s / %s ",
            dto.getTraceId(),
            dto.getPayload().getErrorMessage(),
            dto.getPayload().getErrorStatus()));
  }
}
