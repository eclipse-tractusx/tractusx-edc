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

import org.eclipse.tractusx.edc.cp.adapter.dto.DataReferenceRetrievalDto;
import org.eclipse.tractusx.edc.cp.adapter.service.objectstore.ObjectStoreService;
import org.eclipse.tractusx.edc.cp.adapter.service.objectstore.ObjectType;
import org.eclipse.tractusx.edc.cp.adapter.util.LockMap;

public class ContractSyncService implements ContractNotificationSyncService {
  private final ObjectStoreService storeService;
  private final LockMap locks;

  public ContractSyncService(ObjectStoreService storeService, LockMap locks) {
    this.storeService = storeService;
    this.locks = locks;
  }

  @Override
  public DataReferenceRetrievalDto exchangeConfirmedContract(
      String negotiationId, String agreementId) {
    locks.lock(negotiationId);

    DataReferenceRetrievalDto dto =
        storeService.get(negotiationId, ObjectType.DTO, DataReferenceRetrievalDto.class);

    if (isNull(dto)) {
      ContractInfo contractInfo =
          new ContractInfo(agreementId, ContractInfo.ContractState.CONFIRMED);
      storeService.put(negotiationId, ObjectType.CONTRACT_INFO, contractInfo);
    }
    locks.unlock(negotiationId);
    return dto;
  }

  @Override
  public DataReferenceRetrievalDto exchangeDeclinedContract(String negotiationId) {
    locks.lock(negotiationId);

    DataReferenceRetrievalDto dto =
        storeService.get(negotiationId, ObjectType.DTO, DataReferenceRetrievalDto.class);

    if (isNull(dto)) {
      ContractInfo contractInfo = new ContractInfo(ContractInfo.ContractState.DECLINED);
      storeService.put(negotiationId, ObjectType.CONTRACT_INFO, contractInfo);
    }
    locks.unlock(negotiationId);
    return dto;
  }

  @Override
  public DataReferenceRetrievalDto exchangeErrorContract(String negotiationId) {
    locks.lock(negotiationId);

    DataReferenceRetrievalDto dto =
        storeService.get(negotiationId, ObjectType.DTO, DataReferenceRetrievalDto.class);

    if (isNull(dto)) {
      ContractInfo contractInfo = new ContractInfo(ContractInfo.ContractState.ERROR);
      storeService.put(negotiationId, ObjectType.CONTRACT_INFO, contractInfo);
    }

    locks.unlock(negotiationId);
    return dto;
  }

  @Override
  public ContractInfo exchangeDto(DataReferenceRetrievalDto dto) {
    String negotiationId = dto.getPayload().getContractNegotiationId();
    locks.lock(negotiationId);

    ContractInfo contractInfo =
        storeService.get(negotiationId, ObjectType.CONTRACT_INFO, ContractInfo.class);

    if (isNull(contractInfo)) {
      storeService.put(negotiationId, ObjectType.DTO, dto);
    }

    locks.unlock(negotiationId);
    return contractInfo;
  }

  @Override
  public void removeContractInfo(String negotiationId) {
    storeService.remove(negotiationId, ObjectType.CONTRACT_INFO);
    locks.removeLock(negotiationId);
  }

  @Override
  public void removeDto(String negotiationId) {
    storeService.remove(negotiationId, ObjectType.DTO);
    locks.removeLock(negotiationId);
  }
}
