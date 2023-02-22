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

package org.eclipse.tractusx.edc.cp.adapter.process.contractnegotiation;

import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractOfferRequest;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.tractusx.edc.cp.adapter.dto.DataReferenceRetrievalDto;
import org.eclipse.tractusx.edc.cp.adapter.dto.ProcessData;
import org.eclipse.tractusx.edc.cp.adapter.exception.ResourceNotFoundException;
import org.eclipse.tractusx.edc.cp.adapter.messaging.Channel;
import org.eclipse.tractusx.edc.cp.adapter.messaging.Listener;
import org.eclipse.tractusx.edc.cp.adapter.messaging.MessageBus;

@RequiredArgsConstructor
public class ContractNegotiationHandler implements Listener<DataReferenceRetrievalDto> {
  private final Monitor monitor;
  private final MessageBus messageBus;
  private final ContractNegotiationService contractNegotiationService;
  private final CatalogCachedRetriever catalogRetriever;
  private final ContractAgreementRetriever agreementRetriever;

  @Override
  public void process(DataReferenceRetrievalDto dto) {
    monitor.info(
        String.format(
            "[%s] RequestHandler: input request: [%s]", dto.getTraceId(), dto.getPayload()));
    ProcessData processData = dto.getPayload();

    ContractAgreement contractAgreement = getContractAgreementById(dto);
    if (Objects.nonNull(dto.getPayload().getContractAgreementId()) && contractAgreement == null) {
      sendNotFoundErrorResult(dto, getAgreementNotFoundMessage(dto));
      return;
    }

    if (Objects.isNull(contractAgreement)) {
      contractAgreement =
          agreementRetriever.getExistingContractByAssetId(dto.getPayload().getAssetId());
    }

    if (Objects.nonNull(contractAgreement) && isContractValid(contractAgreement)) {
      monitor.info(
          String.format("[%s] existing ContractAgreement taken from EDC.", dto.getTraceId()));
      dto.getPayload().setContractAgreementId(contractAgreement.getId());
      dto.getPayload().setContractConfirmed(true);
      messageBus.send(Channel.CONTRACT_CONFIRMATION, dto);
      return;
    }

    ContractOffer contractOffer =
        findContractOffer(
            processData.getAssetId(),
            processData.getProvider(),
            processData.getCatalogExpiryTime());

    if (Objects.isNull(contractOffer)) {
      sendNotFoundErrorResult(dto, getContractNotFoundMessage(dto));
      return;
    }

    String contractNegotiationId =
        initializeContractNegotiation(
            contractOffer, dto.getPayload().getProvider(), dto.getTraceId());
    dto.getPayload().setContractNegotiationId(contractNegotiationId);

    messageBus.send(Channel.CONTRACT_CONFIRMATION, dto);
  }

  private ContractAgreement getContractAgreementById(DataReferenceRetrievalDto dto) {
    return Optional.ofNullable(dto.getPayload().getContractAgreementId())
        .map(agreementRetriever::getExistingContractById)
        .orElse(null);
  }

  private boolean isContractValid(ContractAgreement contractAgreement) {
    long now = Instant.now().getEpochSecond();
    return Objects.nonNull(contractAgreement)
        && contractAgreement.getContractStartDate() < now
        && contractAgreement.getContractEndDate() > now;
  }

  private ContractOffer findContractOffer(
      String assetId, String providerUrl, int catalogExpiryTime) {
    Catalog catalog = catalogRetriever.getEntireCatalog(providerUrl, assetId, catalogExpiryTime);
    return Optional.ofNullable(catalog.getContractOffers()).orElse(Collections.emptyList()).stream()
        .filter(it -> it.getAsset().getId().equals(assetId))
        .findFirst()
        .orElse(null);
  }

  private String initializeContractNegotiation(
      ContractOffer contractOffer, String providerUrl, String traceId) {
    monitor.info(String.format("[%s] RequestHandler: initiateNegotiation - start", traceId));
    ContractOfferRequest contractOfferRequest =
        ContractOfferRequest.Builder.newInstance()
            .connectorAddress(providerUrl)
            .contractOffer(contractOffer)
            .type(ContractOfferRequest.Type.INITIAL)
            .connectorId("provider")
            .protocol("ids-multipart")
            .correlationId(traceId)
            .build();

    ContractNegotiation contractNegotiation =
        contractNegotiationService.initiateNegotiation(contractOfferRequest);
    monitor.info(String.format("[%s] RequestHandler: initiateNegotiation - end", traceId));
    return Optional.ofNullable(contractNegotiation.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Could not find Contract NegotiationId"));
  }

  private void sendNotFoundErrorResult(DataReferenceRetrievalDto dto, String message) {
    dto.getPayload().setErrorMessage(message);
    dto.getPayload().setErrorStatus(Response.Status.NOT_FOUND);
    messageBus.send(Channel.RESULT, dto);
  }

  private String getAgreementNotFoundMessage(DataReferenceRetrievalDto dto) {
    return "Not found the contract agreement with ID: " + dto.getPayload().getContractAgreementId();
  }

  private String getContractNotFoundMessage(DataReferenceRetrievalDto dto) {
    return "Could not find Contract Offer for given Asset Id";
  }
}
