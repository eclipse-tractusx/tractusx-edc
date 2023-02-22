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

import static java.util.Objects.isNull;

import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.eclipse.edc.connector.contract.spi.negotiation.observe.ContractNegotiationListener;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.tractusx.edc.cp.adapter.dto.DataReferenceRetrievalDto;
import org.eclipse.tractusx.edc.cp.adapter.messaging.Channel;
import org.eclipse.tractusx.edc.cp.adapter.messaging.MessageBus;

@RequiredArgsConstructor
public class ContractNegotiationListenerImpl implements ContractNegotiationListener {
  public static final String CONTRACT_DECLINED_MESSAGE = "Contract for asset is declined.";
  public static final String CONTRACT_ERROR_MESSAGE = "Contract Error for asset.";
  private final Monitor monitor;
  private final MessageBus messageBus;
  private final ContractNotificationSyncService syncService;
  private final DataTransferInitializer dataTransfer;

  @Override
  public void confirmed(ContractNegotiation negotiation) {
    monitor.info("ContractConfirmationHandler: received ContractConfirmation event");
    String negotiationId = negotiation.getId();
    String agreementId = negotiation.getContractAgreement().getId();
    DataReferenceRetrievalDto dto =
        syncService.exchangeConfirmedContract(negotiationId, agreementId);
    if (isNull(dto)) {
      return;
    }
    dto.getPayload().setContractAgreementId(agreementId);
    initiateDataTransfer(dto);
    syncService.removeDto(negotiationId);
  }

  @Override
  public void declined(ContractNegotiation negotiation) {
    monitor.info("ContractConfirmationHandler: received ContractDeclined event");
    String contractNegotiationId = negotiation.getId();
    DataReferenceRetrievalDto dto = syncService.exchangeDeclinedContract(contractNegotiationId);
    if (isNull(dto)) {
      return;
    }
    sendErrorResult(dto, CONTRACT_DECLINED_MESSAGE);
    syncService.removeDto(contractNegotiationId);
  }

  @Override
  public void failed(ContractNegotiation negotiation) {
    monitor.info("ContractConfirmationHandler: received ContractError event");
    String contractNegotiationId = negotiation.getId();
    DataReferenceRetrievalDto dto = syncService.exchangeErrorContract(contractNegotiationId);
    if (isNull(dto)) {
      return;
    }
    sendErrorResult(dto, CONTRACT_ERROR_MESSAGE);
    syncService.removeDto(contractNegotiationId);
  }

  public void initiateDataTransfer(DataReferenceRetrievalDto dto) {
    String transferProcessId = dataTransfer.initiate(dto);
    dto.getPayload().setTransferProcessId(transferProcessId);
    dto.getPayload().setContractConfirmed(true);
    messageBus.send(Channel.DATA_REFERENCE, dto);
  }

  private void sendErrorResult(DataReferenceRetrievalDto dto, String errorMessage) {
    dto.getPayload().setErrorMessage(errorMessage);
    dto.getPayload().setErrorStatus(Response.Status.BAD_GATEWAY);
    messageBus.send(Channel.RESULT, dto);
  }
}
