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

import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import net.catenax.edc.cp.adapter.dto.DataReferenceRetrievalDto;
import net.catenax.edc.cp.adapter.messaging.Channel;
import net.catenax.edc.cp.adapter.messaging.Listener;
import net.catenax.edc.cp.adapter.messaging.MessageService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceReceiver;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class DataReferenceHandler
    implements Listener<DataReferenceRetrievalDto>, EndpointDataReferenceReceiver {
  private final Monitor monitor;
  private final MessageService messageService;
  private final NotificationSyncService syncService;

  @Override
  public void process(DataReferenceRetrievalDto dto) {
    String contractAgreementId = dto.getPayload().getContractAgreementId();
    monitor.info(String.format("[%s] DataReference message received.", dto.getTraceId()));

    EndpointDataReference dataReference = syncService.exchangeDto(dto, contractAgreementId);
    if (isNull(dataReference)) {
      return;
    }

    dto.getPayload().setEndpointDataReference(dataReference);
    messageService.send(Channel.RESULT, dto);
    syncService.removeDataReference(contractAgreementId);
    monitor.info(String.format("[%s] DataReference message processed.", dto.getTraceId()));
  }

  @Override
  public CompletableFuture<Result<Void>> send(@NotNull EndpointDataReference dataReference) {
    String contractAgreementId = dataReference.getProperties().get("cid");
    monitor.info(String.format("DataReference received, contractAgr.: %s", contractAgreementId));

    DataReferenceRetrievalDto dto =
        syncService.exchangeDataReference(dataReference, contractAgreementId);
    if (isNull(dto)) {
      return CompletableFuture.completedFuture(Result.success());
    }
    dto.getPayload().setEndpointDataReference(dataReference);
    messageService.send(Channel.RESULT, dto);
    syncService.removeDto(contractAgreementId);

    monitor.info(String.format("[%s] DataReference processed.", dto.getTraceId()));
    return CompletableFuture.completedFuture(Result.success());
  }
}
