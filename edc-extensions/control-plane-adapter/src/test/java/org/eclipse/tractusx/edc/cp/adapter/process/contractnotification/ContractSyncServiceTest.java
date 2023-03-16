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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.tractusx.edc.cp.adapter.dto.DataReferenceRetrievalDto;
import org.eclipse.tractusx.edc.cp.adapter.dto.ProcessData;
import org.eclipse.tractusx.edc.cp.adapter.service.objectstore.ObjectStoreServiceInMemory;
import org.eclipse.tractusx.edc.cp.adapter.util.LockMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ContractSyncServiceTest {

  @Test
  public void exchangeConfirmedContract_shouldReturnDtoIfAvailable() {
    // given
    ContractSyncService syncService =
        new ContractSyncService(new ObjectStoreServiceInMemory(new ObjectMapper()), new LockMap());
    syncService.exchangeDto(getDataReferenceRetrievalDto());

    // when
    DataReferenceRetrievalDto dto =
        syncService.exchangeConfirmedContract("negotiationId", "agreementId");

    // then
    Assertions.assertNotNull(dto);
  }

  @Test
  public void exchangeConfirmedContract_shouldReturnNullIfDtoNotAvailable() {
    // given
    ContractSyncService syncService =
        new ContractSyncService(new ObjectStoreServiceInMemory(new ObjectMapper()), new LockMap());

    // when
    DataReferenceRetrievalDto dto =
        syncService.exchangeConfirmedContract("negotiationId", "agreementId");

    // then
    Assertions.assertNull(dto);
  }

  @Test
  public void exchangeDeclinedContract_shouldReturnDtoIfAvailable() {
    // given
    ContractSyncService syncService =
        new ContractSyncService(new ObjectStoreServiceInMemory(new ObjectMapper()), new LockMap());
    syncService.exchangeDto(getDataReferenceRetrievalDto());

    // when
    DataReferenceRetrievalDto dto = syncService.exchangeDeclinedContract("negotiationId");

    // then
    Assertions.assertNotNull(dto);
  }

  @Test
  public void exchangeDeclinedContract_shouldReturnNullIfDtoNotAvailable() {
    // given
    ContractSyncService syncService =
        new ContractSyncService(new ObjectStoreServiceInMemory(new ObjectMapper()), new LockMap());

    // when
    DataReferenceRetrievalDto dto = syncService.exchangeDeclinedContract("negotiationId");

    // then
    Assertions.assertNull(dto);
  }

  @Test
  public void exchangeErrorContract_shouldReturnDtoIfAvailable() {
    // given
    ContractSyncService syncService =
        new ContractSyncService(new ObjectStoreServiceInMemory(new ObjectMapper()), new LockMap());
    syncService.exchangeDto(getDataReferenceRetrievalDto());

    // when
    DataReferenceRetrievalDto dto = syncService.exchangeErrorContract("negotiationId");

    // then
    Assertions.assertNotNull(dto);
  }

  @Test
  public void exchangeErrorContract_shouldReturnNullIfDtoNotAvailable() {
    // given
    ContractSyncService syncService =
        new ContractSyncService(new ObjectStoreServiceInMemory(new ObjectMapper()), new LockMap());

    // when
    DataReferenceRetrievalDto dto = syncService.exchangeErrorContract("negotiationId");

    // then
    Assertions.assertNull(dto);
  }

  @Test
  public void exchangeDto_shouldReturnContractInfoIfAvailable() {
    // given
    ContractSyncService syncService =
        new ContractSyncService(new ObjectStoreServiceInMemory(new ObjectMapper()), new LockMap());
    syncService.exchangeConfirmedContract("negotiationId", "agreementId");

    // when
    ContractInfo contractInfo = syncService.exchangeDto(getDataReferenceRetrievalDto());

    // then
    Assertions.assertNotNull(contractInfo);
  }

  @Test
  public void exchangeDto_shouldReturnNullIfContractInfoNotAvailable() {
    // given
    ContractSyncService syncService =
        new ContractSyncService(new ObjectStoreServiceInMemory(new ObjectMapper()), new LockMap());

    // when
    ContractInfo contractInfo = syncService.exchangeDto(getDataReferenceRetrievalDto());

    // then
    Assertions.assertNull(contractInfo);
  }

  private DataReferenceRetrievalDto getDataReferenceRetrievalDto() {
    ProcessData processData = ProcessData.builder().assetId("assetId").provider("provider").build();
    DataReferenceRetrievalDto dto = new DataReferenceRetrievalDto(processData, 3);
    dto.getPayload().setContractNegotiationId("negotiationId");
    return dto;
  }
}
