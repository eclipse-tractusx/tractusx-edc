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

package org.eclipse.tractusx.edc.cp.adapter.process.contractnotification;

import static jakarta.ws.rs.core.Response.Status;
import static java.util.Objects.isNull;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.tractusx.edc.cp.adapter.dto.DataReferenceRetrievalDto;
import org.eclipse.tractusx.edc.cp.adapter.messaging.Channel;
import org.eclipse.tractusx.edc.cp.adapter.messaging.Listener;
import org.eclipse.tractusx.edc.cp.adapter.messaging.MessageBus;

@RequiredArgsConstructor
public class ContractNotificationHandler implements Listener<DataReferenceRetrievalDto> {
  public static final String CONTRACT_DECLINED_MESSAGE = "Contract for asset is declined.";
  public static final String CONTRACT_ERROR_MESSAGE = "Contract Error for asset.";
  private final Monitor monitor;
  private final MessageBus messageBus;
  private final ContractNotificationSyncService syncService;
  private final ContractNegotiationService contractNegotiationService;
  private final DataTransferInitializer dataTransfer;

  @Override
  public void process(DataReferenceRetrievalDto dto) {
    monitor.info(
        String.format("[%s] ContractConfirmationHandler: received message.", dto.getTraceId()));
    String contractNegotiationId = dto.getPayload().getContractNegotiationId();

    if (dto.getPayload().isContractConfirmed()) {
      initiateDataTransfer(dto);
      return;
    }

    ContractNegotiation contractNegotiation =
        contractNegotiationService.findbyId(contractNegotiationId);
    if (isContractConfirmed(contractNegotiation)) {
      dto.getPayload().setContractAgreementId(contractNegotiation.getContractAgreement().getId());
      initiateDataTransfer(dto);
      return;
    }

    ContractInfo contractInfo = syncService.exchangeDto(dto);
    if (isNull(contractInfo)) {
      return;
    }

    if (contractInfo.isConfirmed()) {
      dto.getPayload().setContractAgreementId(contractInfo.getContractAgreementId());
      initiateDataTransfer(dto);
    } else {
      sendErrorResult(
          dto, contractInfo.isDeclined() ? CONTRACT_DECLINED_MESSAGE : CONTRACT_ERROR_MESSAGE);
    }
    syncService.removeContractInfo(contractNegotiationId);
  }

  public void initiateDataTransfer(DataReferenceRetrievalDto dto) {
    String transferProcessId = dataTransfer.initiate(dto);
    dto.getPayload().setTransferProcessId(transferProcessId);
    dto.getPayload().setContractConfirmed(true);
    messageBus.send(Channel.DATA_REFERENCE, dto);
  }

  private void sendErrorResult(DataReferenceRetrievalDto dto, String errorMessage) {
    dto.getPayload().setErrorMessage(errorMessage);
    dto.getPayload().setErrorStatus(Status.BAD_GATEWAY);
    messageBus.send(Channel.RESULT, dto);
  }

  private boolean isContractConfirmed(ContractNegotiation contractNegotiation) {
    return Objects.nonNull(contractNegotiation)
        && contractNegotiation.getState() == ContractNegotiationStates.CONFIRMED.code();
  }
}
