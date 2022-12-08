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

package net.catenax.edc.cp.adapter.process.contractnegotiation;

import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import lombok.RequiredArgsConstructor;
import net.catenax.edc.cp.adapter.dto.DataReferenceRetrievalDto;
import net.catenax.edc.cp.adapter.dto.ProcessData;
import net.catenax.edc.cp.adapter.exception.ExternalRequestException;
import net.catenax.edc.cp.adapter.exception.ResourceNotFoundException;
import net.catenax.edc.cp.adapter.messaging.Channel;
import net.catenax.edc.cp.adapter.messaging.Listener;
import net.catenax.edc.cp.adapter.messaging.MessageBus;
import net.catenax.edc.cp.adapter.process.contractdatastore.ContractAgreementData;
import net.catenax.edc.cp.adapter.process.contractdatastore.ContractDataStore;
import net.catenax.edc.cp.adapter.util.ExpiringMap;
import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractOfferRequest;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.spi.catalog.CatalogService;
import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public class ContractNegotiationHandler implements Listener<DataReferenceRetrievalDto> {
  private final Monitor monitor;
  private final MessageBus messageBus;
  private final ContractNegotiationService contractNegotiationService;
  private final CatalogService catalogService;
  private final ContractDataStore contractDataStore;
  private final ExpiringMap<String, Catalog> catalogCache;

  @Override
  public void process(DataReferenceRetrievalDto dto) {
    monitor.info(
        String.format(
            "[%s] RequestHandler: input request: [%s]", dto.getTraceId(), dto.getPayload()));
    ProcessData processData = dto.getPayload();

    ContractAgreementData contractData = getCachedContractData(dto);
    if (Objects.nonNull(contractData) && isContractValid(contractData)) {
      monitor.info(String.format("[%s] ContractAgreement taken from cache.", dto.getTraceId()));
      dto.getPayload().setContractAgreementId(contractData.getId());
      dto.getPayload().setContractConfirmed(true);
      messageBus.send(Channel.CONTRACT_CONFIRMATION, dto);
      return;
    }

    ContractOffer contractOffer =
        findContractOffer(
            processData.getAssetId(),
            processData.getProvider(),
            processData.getCatalogExpiryTime());

    String contractNegotiationId =
        initializeContractNegotiation(
            contractOffer, dto.getPayload().getProvider(), dto.getTraceId());
    dto.getPayload().setContractNegotiationId(contractNegotiationId);

    messageBus.send(Channel.CONTRACT_CONFIRMATION, dto);
  }

  @Nullable
  private ContractAgreementData getCachedContractData(DataReferenceRetrievalDto dto) {
    return dto.getPayload().isContractAgreementCacheOn()
        ? contractDataStore.get(dto.getPayload().getAssetId(), dto.getPayload().getProvider())
        : null;
  }

  private boolean isContractValid(ContractAgreementData contractAgreement) {
    long now = Instant.now().getEpochSecond();
    return Objects.nonNull(contractAgreement)
        && contractAgreement.getContractStartDate() < now
        && contractAgreement.getContractEndDate() > now;
  }

  private ContractOffer findContractOffer(
      String assetId, String providerUrl, int catalogExpiryTime) {
    Catalog catalog = getCatalog(providerUrl, catalogExpiryTime);
    return Optional.ofNullable(catalog.getContractOffers()).orElse(Collections.emptyList()).stream()
        .filter(it -> it.getAsset().getId().equals(assetId))
        .findFirst()
        .orElseThrow(
            () ->
                new ResourceNotFoundException("Could not find Contract Offer for given Asset Id"));
  }

  private Catalog getCatalog(String providerUrl, int catalogExpiryTime) {
    Catalog catalog = catalogCache.get(providerUrl, catalogExpiryTime);
    if (Objects.nonNull(catalog)) {
      return catalog;
    }

    try {
      catalog = catalogService.getByProviderUrl(providerUrl, QuerySpec.max()).get();
      catalogCache.put(providerUrl, catalog);
      return catalog;
    } catch (InterruptedException | ExecutionException e) {
      throw new ExternalRequestException("Could not retrieve contract offer.", e);
    }
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
}
