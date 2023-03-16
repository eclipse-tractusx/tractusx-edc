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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.tractusx.edc.cp.adapter.dto.DataReferenceRetrievalDto;
import org.eclipse.tractusx.edc.cp.adapter.dto.ProcessData;
import org.eclipse.tractusx.edc.cp.adapter.messaging.Message;
import org.eclipse.tractusx.edc.cp.adapter.messaging.MessageBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ContractNegotiationHandlerTest {
  @Mock Monitor monitor;
  @Mock MessageBus messageBus;
  @Mock ContractNegotiationService contractNegotiationService;
  @Mock CatalogCachedRetriever catalogRetriever;
  @Mock ContractAgreementRetriever agreementRetriever;

  @BeforeEach
  void init() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void process_shouldNotInitializeContractNegotiationWhenContractAlreadyAvailable() {
    // given
    ContractNegotiationHandler contractNegotiationHandler =
        new ContractNegotiationHandler(
            monitor, messageBus, contractNegotiationService, catalogRetriever, agreementRetriever);

    when(agreementRetriever.getExistingContractByAssetId(anyString()))
        .thenReturn(getValidContractAgreement());

    // when
    contractNegotiationHandler.process(new DataReferenceRetrievalDto(getProcessData(), 3));

    // then
    verify(contractNegotiationService, times(0)).initiateNegotiation(any());
    verify(messageBus, times(1)).send(any(), any(Message.class));
  }

  @Test
  public void process_shouldInitializeContractNegotiationWhenExistingContractExpired() {
    // given
    ContractNegotiationHandler contractNegotiationHandler =
        new ContractNegotiationHandler(
            monitor, messageBus, contractNegotiationService, catalogRetriever, agreementRetriever);

    when(agreementRetriever.getExistingContractByAssetId(anyString()))
        .thenReturn(getExpiredContractAgreement());
    when(catalogRetriever.getEntireCatalog(anyString(), anyString(), anyInt()))
        .thenReturn(getCatalog());
    when(contractNegotiationService.initiateNegotiation(any()))
        .thenReturn(getContractNegotiation());

    // when
    contractNegotiationHandler.process(new DataReferenceRetrievalDto(getProcessData(), 3));

    // then
    verify(contractNegotiationService, times(1)).initiateNegotiation(any());
    verify(messageBus, times(1)).send(any(), any(Message.class));
  }

  @Test
  public void process_shouldInitiateContractNegotiationAndSendDtoFurtherIfAgreementNotExist() {
    // given
    ContractNegotiationHandler contractNegotiationHandler =
        new ContractNegotiationHandler(
            monitor, messageBus, contractNegotiationService, catalogRetriever, agreementRetriever);

    when(agreementRetriever.getExistingContractByAssetId(anyString())).thenReturn(null);
    when(catalogRetriever.getEntireCatalog(anyString(), anyString(), anyInt()))
        .thenReturn(getCatalog());
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
        .contractAgreementReuseOn(true)
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
        .contractStart(ZonedDateTime.now())
        .contractEnd(ZonedDateTime.now().plusDays(1))
        .policy(Policy.Builder.newInstance().build())
        .build();
  }

  private ContractAgreement getValidContractAgreement() {
    long now = Instant.now().getEpochSecond();
    return ContractAgreement.Builder.newInstance()
        .id("id")
        .assetId("assetId")
        .contractStartDate(now - 5000)
        .contractEndDate(now + 5000)
        .consumerAgentId("consumer")
        .providerAgentId("provider")
        .policy(Policy.Builder.newInstance().build())
        .build();
  }

  private ContractAgreement getExpiredContractAgreement() {
    long now = Instant.now().getEpochSecond();
    return ContractAgreement.Builder.newInstance()
        .id("id")
        .assetId("assetId")
        .contractStartDate(now - 5000)
        .contractEndDate(now - 1000)
        .consumerAgentId("consumer")
        .providerAgentId("provider")
        .policy(Policy.Builder.newInstance().build())
        .build();
  }
}
