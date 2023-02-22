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

package org.eclipse.tractusx.edc.cp.adapter.process.datareference;

import static java.util.Objects.isNull;

import lombok.RequiredArgsConstructor;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.tractusx.edc.cp.adapter.dto.DataReferenceRetrievalDto;
import org.eclipse.tractusx.edc.cp.adapter.service.objectstore.ObjectStoreService;
import org.eclipse.tractusx.edc.cp.adapter.service.objectstore.ObjectType;
import org.eclipse.tractusx.edc.cp.adapter.util.LockMap;

@RequiredArgsConstructor
public class DataRefSyncService implements DataRefNotificationSyncService {
  private final ObjectStoreService storeService;
  private final LockMap locks;

  public EndpointDataReference exchangeDto(DataReferenceRetrievalDto dto, String agreementId) {
    locks.lock(agreementId);

    EndpointDataReference dataReference =
        storeService.get(agreementId, ObjectType.DATA_REFERENCE, EndpointDataReference.class);

    if (isNull(dataReference)) {
      storeService.put(agreementId, ObjectType.DTO, dto);
    }
    locks.unlock(agreementId);
    return dataReference;
  }

  @Override
  public DataReferenceRetrievalDto exchangeDataReference(
      EndpointDataReference dataReference, String agreementId) {
    locks.lock(agreementId);

    DataReferenceRetrievalDto dto =
        storeService.get(agreementId, ObjectType.DTO, DataReferenceRetrievalDto.class);

    if (isNull(dto)) {
      storeService.put(agreementId, ObjectType.DATA_REFERENCE, dataReference);
    }
    locks.unlock(agreementId);
    return dto;
  }

  @Override
  public void removeDataReference(String agreementId) {
    storeService.remove(agreementId, ObjectType.DATA_REFERENCE);
    locks.removeLock(agreementId);
  }

  @Override
  public void removeDto(String agreementId) {
    storeService.remove(agreementId, ObjectType.DTO);
    locks.removeLock(agreementId);
  }
}
