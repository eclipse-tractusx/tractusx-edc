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

package net.catenax.edc.cp.adapter.process.contractnotification;

import static jakarta.ws.rs.core.Response.Status;
import static java.util.Objects.isNull;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import net.catenax.edc.cp.adapter.dto.DataReferenceRetrievalDto;
import net.catenax.edc.cp.adapter.exception.ExternalRequestException;
import net.catenax.edc.cp.adapter.messaging.Channel;
import net.catenax.edc.cp.adapter.messaging.Listener;
import net.catenax.edc.cp.adapter.messaging.MessageService;
import net.catenax.edc.cp.adapter.process.contractdatastore.ContractDataStore;
import org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.service.ContractNegotiationService;
import org.eclipse.dataspaceconnector.api.datamanagement.transferprocess.service.TransferProcessService;
import org.eclipse.dataspaceconnector.api.result.ServiceResult;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.observe.ContractNegotiationListener;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferType;

@RequiredArgsConstructor
public class ContractNotificationHandler
    implements Listener<DataReferenceRetrievalDto>, ContractNegotiationListener {
  public static final String CONTRACT_DECLINED_MESSAGE = "Contract for asset is declined.";
  public static final String CONTRACT_ERROR_MESSAGE = "Contract Error for asset.";
  private final Monitor monitor;
  private final MessageService messageService;
  private final NotificationSyncService syncService;
  private final ContractNegotiationService contractNegotiationService;
  private final TransferProcessService transferProcessService;
  private final ContractDataStore contractDataStore;

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
          dto,
          Status.BAD_GATEWAY,
          contractInfo.isDeclined() ? CONTRACT_DECLINED_MESSAGE : CONTRACT_ERROR_MESSAGE);
    }
    syncService.removeContractInfo(contractNegotiationId);
  }

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
    contractDataStore.add(
        dto.getPayload().getAssetId(),
        dto.getPayload().getProvider(),
        negotiation.getContractAgreement());
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
    sendErrorResult(dto, Status.BAD_GATEWAY, CONTRACT_DECLINED_MESSAGE);
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
    sendErrorResult(dto, Status.BAD_GATEWAY, CONTRACT_ERROR_MESSAGE);
    syncService.removeDto(contractNegotiationId);
  }

  private void initiateDataTransfer(DataReferenceRetrievalDto dto) {
    sendInitiationRequest(dto);
    dto.getPayload().setContractConfirmed(true);
    messageService.send(Channel.DATA_REFERENCE, dto);
  }

  private void sendInitiationRequest(DataReferenceRetrievalDto dto) {
    monitor.info(
        String.format(
            "[%s] ContractConfirmationHandler: transfer init - start.", dto.getTraceId()));
    DataAddress dataDestination = DataAddress.Builder.newInstance().type("HttpProxy").build();

    TransferType transferType =
        TransferType.Builder.transferType()
            .contentType("application/octet-stream")
            .isFinite(true)
            .build();

    DataRequest dataRequest =
        DataRequest.Builder.newInstance()
            .id(dto.getTraceId())
            .assetId(dto.getPayload().getAssetId())
            .contractId(dto.getPayload().getContractAgreementId())
            .connectorId("provider")
            .connectorAddress(dto.getPayload().getProvider())
            .protocol("ids-multipart")
            .dataDestination(dataDestination)
            .managedResources(false)
            .transferType(transferType)
            .build();

    ServiceResult<String> result = transferProcessService.initiateTransfer(dataRequest);
    monitor.info(
        String.format("[%s] ContractConfirmationHandler: transfer init - end", dto.getTraceId()));
    if (result.failed()) {
      throwDataRefRequestException(dto);
    }
  }

  private void throwDataRefRequestException(DataReferenceRetrievalDto dto) {
    throw new ExternalRequestException(
        String.format(
            "Data reference initial request failed! AssetId: %s", dto.getPayload().getAssetId()));
  }

  private void sendErrorResult(DataReferenceRetrievalDto dto, Status status, String errorMessage) {
    dto.getPayload().setErrorMessage(errorMessage);
    dto.getPayload().setErrorStatus(status);
    messageService.send(Channel.RESULT, dto);
  }

  private boolean isContractConfirmed(ContractNegotiation contractNegotiation) {
    return Objects.nonNull(contractNegotiation)
        && contractNegotiation.getState() == ContractNegotiationStates.CONFIRMED.code();
  }
}
