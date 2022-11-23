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

import static java.util.Objects.isNull;

import java.util.HashMap;
import java.util.Map;
import net.catenax.edc.cp.adapter.dto.DataReferenceRetrievalDto;
import net.catenax.edc.cp.adapter.util.LockMap;

public class ContractInMemorySyncService implements ContractNotificationSyncService {
  private final Map<String, DataReferenceRetrievalDto> dtoMap = new HashMap<>();
  private final Map<String, ContractInfo> contractInfoMap = new HashMap<>();
  private final LockMap locks;

  public ContractInMemorySyncService(LockMap locks) {
    this.locks = locks;
  }

  @Override
  public DataReferenceRetrievalDto exchangeConfirmedContract(
      String negotiationId, String agreementId) {
    locks.lock(negotiationId);
    DataReferenceRetrievalDto dto = dtoMap.get(negotiationId);
    if (isNull(dto)) {
      contractInfoMap.put(
          negotiationId, new ContractInfo(agreementId, ContractInfo.ContractState.CONFIRMED));
    }
    locks.unlock(negotiationId);
    return dto;
  }

  @Override
  public DataReferenceRetrievalDto exchangeDeclinedContract(String negotiationId) {
    locks.lock(negotiationId);
    DataReferenceRetrievalDto dto = dtoMap.get(negotiationId);
    if (isNull(dto)) {
      contractInfoMap.put(negotiationId, new ContractInfo(ContractInfo.ContractState.DECLINED));
    }
    locks.unlock(negotiationId);
    return dto;
  }

  @Override
  public DataReferenceRetrievalDto exchangeErrorContract(String negotiationId) {
    locks.lock(negotiationId);
    DataReferenceRetrievalDto dto = dtoMap.get(negotiationId);
    if (isNull(dto)) {
      contractInfoMap.put(negotiationId, new ContractInfo(ContractInfo.ContractState.ERROR));
    }

    locks.unlock(negotiationId);
    return dto;
  }

  @Override
  public ContractInfo exchangeDto(DataReferenceRetrievalDto dto) {
    String negotiationId = dto.getPayload().getContractNegotiationId();

    locks.lock(negotiationId);
    ContractInfo contractInfo = contractInfoMap.get(negotiationId);
    if (isNull(contractInfo)) {
      dtoMap.put(negotiationId, dto);
    }

    locks.unlock(negotiationId);
    return contractInfo;
  }

  @Override
  public void removeContractInfo(String negotiationId) {
    contractInfoMap.remove(negotiationId);
    locks.removeLock(negotiationId);
  }

  @Override
  public void removeDto(String negotiationId) {
    dtoMap.remove(negotiationId);
    locks.removeLock(negotiationId);
  }
}
