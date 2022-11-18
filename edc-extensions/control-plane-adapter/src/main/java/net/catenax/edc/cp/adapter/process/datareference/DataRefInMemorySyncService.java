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

package net.catenax.edc.cp.adapter.process.datareference;

import static java.util.Objects.isNull;

import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import net.catenax.edc.cp.adapter.dto.DataReferenceRetrievalDto;
import net.catenax.edc.cp.adapter.util.LockMap;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;

@RequiredArgsConstructor
public class DataRefInMemorySyncService implements DataRefNotificationSyncService {
  private final Map<String, DataReferenceRetrievalDto> dtoMap = new HashMap<>();
  private final Map<String, EndpointDataReference> dataReferenceMap = new HashMap<>();
  private final LockMap locks;

  public EndpointDataReference exchangeDto(DataReferenceRetrievalDto dto, String agreementId) {
    locks.lock(agreementId);
    EndpointDataReference dataReference = dataReferenceMap.get(agreementId);
    if (isNull(dataReference)) {
      dtoMap.put(agreementId, dto);
    }
    locks.unlock(agreementId);
    return dataReference;
  }

  @Override
  public DataReferenceRetrievalDto exchangeDataReference(
      EndpointDataReference dataReference, String agreementId) {
    locks.lock(agreementId);
    DataReferenceRetrievalDto dto = dtoMap.get(agreementId);
    if (isNull(dto)) {
      dataReferenceMap.put(agreementId, dataReference);
    }
    locks.unlock(agreementId);
    return dto;
  }

  @Override
  public void removeDataReference(String agreementId) {
    dataReferenceMap.remove(agreementId);
    locks.removeLock(agreementId);
  }

  @Override
  public void removeDto(String agreementId) {
    dtoMap.remove(agreementId);
    locks.removeLock(agreementId);
  }
}
