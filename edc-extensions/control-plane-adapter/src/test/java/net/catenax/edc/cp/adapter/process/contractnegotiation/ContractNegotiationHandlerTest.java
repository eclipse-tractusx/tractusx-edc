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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.catenax.edc.cp.adapter.dto.DataReferenceRetrievalDto;
import net.catenax.edc.cp.adapter.dto.ProcessData;
import net.catenax.edc.cp.adapter.messaging.Message;
import net.catenax.edc.cp.adapter.messaging.MessageBus;
import net.catenax.edc.cp.adapter.process.contractdatastore.ContractAgreementData;
import net.catenax.edc.cp.adapter.process.contractdatastore.ContractDataStore;
import net.catenax.edc.cp.adapter.util.ExpiringMap;
import org.eclipse.dataspaceconnector.api.datamanagement.catalog.service.CatalogService;
import org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.service.ContractNegotiationService;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.Catalog;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ContractNegotiationHandlerTest {
  @Mock Monitor monitor;
  @Mock MessageBus messageBus;
  @Mock ContractNegotiationService contractNegotiationService;
  @Mock CatalogService catalogService;
  @Mock ContractDataStore contractDataStore;

  @BeforeEach
  void init() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void process_shouldNotInitializeContractNegotiationWhenCachedContractAlreadyAvailable() {
    // given
    ContractNegotiationHandler contractNegotiationHandler =
        new ContractNegotiationHandler(
            monitor,
            messageBus,
            contractNegotiationService,
            catalogService,
            contractDataStore,
            new ExpiringMap<>());

    when(contractDataStore.get(anyString(), anyString()))
        .thenReturn(getValidContractAgreementData());

    // when
    contractNegotiationHandler.process(new DataReferenceRetrievalDto(getProcessData(), 3));

    // then
    verify(contractNegotiationService, times(0)).initiateNegotiation(any());
    verify(messageBus, times(1)).send(any(), any(Message.class));
  }

  @Test
  public void process_shouldInitializeContractNegotiationWhenCachedContractExpired() {
    // given
    ContractNegotiationHandler contractNegotiationHandler =
        new ContractNegotiationHandler(
            monitor,
            messageBus,
            contractNegotiationService,
            catalogService,
            contractDataStore,
            new ExpiringMap<>());

    when(contractDataStore.get(anyString(), anyString()))
        .thenReturn(getExpiredContractAgreementData());
    when(catalogService.getByProviderUrl(anyString(), any()))
        .thenReturn(CompletableFuture.completedFuture(getCatalog()));
    when(contractNegotiationService.initiateNegotiation(any()))
        .thenReturn(getContractNegotiation());

    // when
    contractNegotiationHandler.process(new DataReferenceRetrievalDto(getProcessData(), 3));

    // then
    verify(contractNegotiationService, times(1)).initiateNegotiation(any());
    verify(messageBus, times(1)).send(any(), any(Message.class));
  }

  @Test
  public void process_shouldInitiateContractNegotiationAndSendDtoFurtherIfCacheEmpty() {
    // given
    ContractNegotiationHandler contractNegotiationHandler =
        new ContractNegotiationHandler(
            monitor,
            messageBus,
            contractNegotiationService,
            catalogService,
            contractDataStore,
            new ExpiringMap<>());

    when(contractDataStore.get(anyString(), anyString())).thenReturn(null);
    when(catalogService.getByProviderUrl(anyString(), any()))
        .thenReturn(CompletableFuture.completedFuture(getCatalog()));
    when(contractNegotiationService.initiateNegotiation(any()))
        .thenReturn(getContractNegotiation());

    // when
    contractNegotiationHandler.process(new DataReferenceRetrievalDto(getProcessData(), 3));

    // then
    verify(contractNegotiationService, times(1)).initiateNegotiation(any());
    verify(messageBus, times(1)).send(any(), any(Message.class));
  }

  private ProcessData getProcessData() {
    return ProcessData.builder()
        .assetId("assetId")
        .provider("provider")
        .catalogExpiryTime(30)
        .contractAgreementCacheOn(true)
        .build();
  }

  private ContractNegotiation getContractNegotiation() {
    return ContractNegotiation.Builder.newInstance()
        .id("contractNegotiationId")
        .counterPartyId("counterPartyId")
        .counterPartyAddress("counterPartyAddress")
        .protocol("protocol")
        .build();
  }

  private Catalog getCatalog() {
    return Catalog.Builder.newInstance()
        .id("id")
        .contractOffers(List.of(getContractOffer()))
        .build();
  }

  private ContractOffer getContractOffer() {
    Asset asset = Asset.Builder.newInstance().id("assetId").build();
    return ContractOffer.Builder.newInstance()
        .id("id")
        .asset(asset)
        .policy(Policy.Builder.newInstance().build())
        .build();
  }

  private ContractAgreementData getValidContractAgreementData() {
    long now = Instant.now().getEpochSecond();
    ContractAgreementData contractAgreementData = new ContractAgreementData();
    contractAgreementData.setId("id");
    contractAgreementData.setAssetId("assetId");
    contractAgreementData.setContractStartDate(now - 5000);
    contractAgreementData.setContractEndDate(now + 5000);
    return contractAgreementData;
  }

  private ContractAgreementData getExpiredContractAgreementData() {
    long now = Instant.now().getEpochSecond();
    ContractAgreementData contractAgreementData = getValidContractAgreementData();
    contractAgreementData.setContractEndDate(now - 1000);
    return contractAgreementData;
  }
}
