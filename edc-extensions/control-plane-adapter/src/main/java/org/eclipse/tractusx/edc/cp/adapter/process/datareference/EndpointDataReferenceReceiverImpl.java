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

import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.eclipse.edc.connector.transfer.spi.edr.EndpointDataReferenceReceiver;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.tractusx.edc.cp.adapter.dto.DataReferenceRetrievalDto;
import org.eclipse.tractusx.edc.cp.adapter.messaging.Channel;
import org.eclipse.tractusx.edc.cp.adapter.messaging.MessageBus;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class EndpointDataReferenceReceiverImpl implements EndpointDataReferenceReceiver {
  private final Monitor monitor;
  private final MessageBus messageBus;
  private final DataRefNotificationSyncService syncService;

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
    messageBus.send(Channel.RESULT, dto);
    syncService.removeDto(contractAgreementId);

    monitor.info(String.format("[%s] DataReference processed.", dto.getTraceId()));
    return CompletableFuture.completedFuture(Result.success());
  }
}
