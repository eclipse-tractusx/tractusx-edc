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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.tractusx.edc.cp.adapter.dto.DataReferenceRetrievalDto;
import org.eclipse.tractusx.edc.cp.adapter.dto.ProcessData;
import org.eclipse.tractusx.edc.cp.adapter.service.objectstore.ObjectStoreServiceInMemory;
import org.eclipse.tractusx.edc.cp.adapter.util.LockMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DataRefSyncServiceTest {

  @Test
  public void exchangeDto_shouldReturnDataReferenceIfAvailable() {
    // given
    DataRefSyncService syncService =
        new DataRefSyncService(new ObjectStoreServiceInMemory(new ObjectMapper()), new LockMap());
    syncService.exchangeDataReference(getEndpointDataReference(), "agreementId");

    // when
    EndpointDataReference dataReference =
        syncService.exchangeDto(getDataReferenceRetrievalDto(), "agreementId");

    // then
    Assertions.assertNotNull(dataReference);
  }

  @Test
  public void exchangeDto_shouldReturnNullIfDataReferenceNotAvailable() {
    // given
    DataRefSyncService syncService =
        new DataRefSyncService(new ObjectStoreServiceInMemory(new ObjectMapper()), new LockMap());

    // when
    EndpointDataReference dataReference =
        syncService.exchangeDto(getDataReferenceRetrievalDto(), "agreementId");

    // then
    Assertions.assertNull(dataReference);
  }

  @Test
  public void exchangeDataReference_shouldReturnDtoIfAvailable() {
    // given
    DataRefSyncService syncService =
        new DataRefSyncService(new ObjectStoreServiceInMemory(new ObjectMapper()), new LockMap());
    syncService.exchangeDto(getDataReferenceRetrievalDto(), "agreementId");

    // when
    DataReferenceRetrievalDto dto =
        syncService.exchangeDataReference(getEndpointDataReference(), "agreementId");

    // then
    Assertions.assertNotNull(dto);
  }

  @Test
  public void exchangeDataReference_shouldReturnNullIfDtoNotAvailable() {
    // given
    DataRefSyncService syncService =
        new DataRefSyncService(new ObjectStoreServiceInMemory(new ObjectMapper()), new LockMap());

    // when
    DataReferenceRetrievalDto dto =
        syncService.exchangeDataReference(getEndpointDataReference(), "agreementId");

    // then
    Assertions.assertNull(dto);
  }

  private EndpointDataReference getEndpointDataReference() {
    return EndpointDataReference.Builder.newInstance()
        .endpoint("endpoint")
        .authCode("authCode")
        .authKey("authKey")
        .build();
  }

  private DataReferenceRetrievalDto getDataReferenceRetrievalDto() {
    ProcessData processData = ProcessData.builder().assetId("assetId").provider("provider").build();
    return new DataReferenceRetrievalDto(processData, 3);
  }
}
