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
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.tractusx.edc.cp.adapter.dto.DataReferenceRetrievalDto;
import org.eclipse.tractusx.edc.cp.adapter.messaging.Channel;
import org.eclipse.tractusx.edc.cp.adapter.messaging.Listener;
import org.eclipse.tractusx.edc.cp.adapter.messaging.MessageBus;

@RequiredArgsConstructor
public class DataReferenceHandler implements Listener<DataReferenceRetrievalDto> {
  private final Monitor monitor;
  private final MessageBus messageBus;
  private final DataRefNotificationSyncService syncService;

  @Override
  public void process(DataReferenceRetrievalDto dto) {
    String contractAgreementId = dto.getPayload().getContractAgreementId();
    monitor.info(String.format("[%s] DataReference message received.", dto.getTraceId()));

    EndpointDataReference dataReference = syncService.exchangeDto(dto, contractAgreementId);
    if (isNull(dataReference)) {
      return;
    }

    dto.getPayload().setEndpointDataReference(dataReference);
    messageBus.send(Channel.RESULT, dto);
    syncService.removeDataReference(contractAgreementId);
    monitor.info(String.format("[%s] DataReference message processed.", dto.getTraceId()));
  }
}
