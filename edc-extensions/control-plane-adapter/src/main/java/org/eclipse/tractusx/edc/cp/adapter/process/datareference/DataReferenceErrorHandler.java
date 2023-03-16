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

package org.eclipse.tractusx.edc.cp.adapter.process.datareference;

import jakarta.ws.rs.core.Response;
import java.util.List;
import lombok.AllArgsConstructor;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.util.string.StringUtils;
import org.eclipse.tractusx.edc.cp.adapter.dto.DataReferenceRetrievalDto;
import org.eclipse.tractusx.edc.cp.adapter.messaging.Channel;
import org.eclipse.tractusx.edc.cp.adapter.messaging.MessageBus;
import org.eclipse.tractusx.edc.cp.adapter.service.objectstore.ObjectStoreService;
import org.eclipse.tractusx.edc.cp.adapter.service.objectstore.ObjectType;

@AllArgsConstructor
public class DataReferenceErrorHandler {
  private static final String ERROR_MESSAGE = "Data reference process stage failed with status: ";
  private final Monitor monitor;
  private final MessageBus messageBus;
  private final ObjectStoreService objectStore;
  private final TransferProcessService transferProcessService;

  private final List<String> errorStates = List.of("CANCELLED", "ERROR");

  public void validateActiveProcesses() {
    monitor.debug("Data reference error handling - START");
    objectStore.get(ObjectType.DTO, DataReferenceRetrievalDto.class).stream()
        .filter(dto -> !StringUtils.isNullOrEmpty(dto.getPayload().getTransferProcessId()))
        .forEach(this::validateProcess);
  }

  private void validateProcess(DataReferenceRetrievalDto dto) {
    String state = transferProcessService.getState(dto.getPayload().getTransferProcessId());
    if (errorStates.contains(state)) {
      monitor.warning(String.format("[%s] ", dto.getTraceId()) + ERROR_MESSAGE + state);
      String contractAgreementId = dto.getPayload().getContractAgreementId();
      objectStore.remove(contractAgreementId, ObjectType.DTO);

      dto.getPayload().setErrorStatus(Response.Status.BAD_GATEWAY);
      dto.getPayload().setErrorMessage(ERROR_MESSAGE + state);
      messageBus.send(Channel.RESULT, dto);
    }
  }
}
