/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.callback;

import org.eclipse.edc.connector.contract.spi.event.contractnegotiation.ContractNegotiationFinalized;
import org.eclipse.edc.connector.spi.callback.CallbackEventRemoteMessage;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.TransferRequest;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.tractusx.edc.spi.callback.InProcessCallback;

import java.util.UUID;

import static java.lang.String.format;

public class ContractNegotiationCallback implements InProcessCallback {

    public static final DataAddress DATA_DESTINATION = DataAddress.Builder.newInstance().type("HttpProxy").build();
    private final TransferProcessService transferProcessService;

    private final Monitor monitor;

    public ContractNegotiationCallback(TransferProcessService transferProcessService, Monitor monitor) {
        this.transferProcessService = transferProcessService;
        this.monitor = monitor;
    }

    @Override
    public <T extends Event> Result<Void> invoke(CallbackEventRemoteMessage<T> message) {
        if (message.getEventEnvelope().getPayload() instanceof ContractNegotiationFinalized) {
            return initiateTransfer((ContractNegotiationFinalized) message.getEventEnvelope().getPayload());
        }
        return Result.success();
    }

    private Result<Void> initiateTransfer(ContractNegotiationFinalized negotiationFinalized) {

        var dataRequest =
                DataRequest.Builder.newInstance()
                        .id(UUID.randomUUID().toString())
                        .assetId(negotiationFinalized.getContractAgreement().getAssetId())
                        .contractId(negotiationFinalized.getContractAgreement().getId())
                        .connectorId(negotiationFinalized.getCounterPartyId())
                        .connectorAddress(negotiationFinalized.getCounterPartyAddress())
                        .protocol(negotiationFinalized.getProtocol())
                        .dataDestination(DATA_DESTINATION)
                        .managedResources(false)
                        .build();

        var transferRequest = TransferRequest.Builder.newInstance()
                .dataRequest(dataRequest)
                .callbackAddresses(negotiationFinalized.getCallbackAddresses())
                .build();

        var result = transferProcessService.initiateTransfer(transferRequest);

        if (result.failed()) {
            var msg = format("Failed to initiate a transfer for contract %s and asset %s, error: %s", negotiationFinalized.getContractAgreement().getId(), negotiationFinalized.getContractAgreement().getAssetId(), result.getFailureDetail());
            monitor.severe(msg);
            return Result.failure(msg);
        }
        monitor.debug(format("Transfer with id %s initiated", result.getContent()));
        return Result.success();
    }
}
