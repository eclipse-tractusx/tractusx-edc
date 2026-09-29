/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.tractusx.edc.callback;

import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationFinalized;
import org.eclipse.edc.connector.controlplane.services.spi.callback.CallbackEventRemoteMessage;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextSupplier;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.tractusx.edc.spi.callback.InProcessCallback;

import java.util.UUID;

import static java.lang.String.format;

public class ContractNegotiationCallback implements InProcessCallback {

    public static final DataAddress DATA_DESTINATION = DataAddress.Builder.newInstance().type("HttpProxy").build();
    private static final String TRANSFER_TYPE = "HttpData-PULL";

    private final TransferProcessService transferProcessService;
    private final Monitor monitor;
    private final ParticipantContextSupplier participantContextSupplier;

    public ContractNegotiationCallback(TransferProcessService transferProcessService, Monitor monitor,
                                       ParticipantContextSupplier participantContextSupplier) {
        this.transferProcessService = transferProcessService;
        this.monitor = monitor.withPrefix(getClass().getSimpleName());
        this.participantContextSupplier = participantContextSupplier;
    }

    @Override
    public <T extends Event> Result<Void> invoke(CallbackEventRemoteMessage<T> message) {
        if (message.getEventEnvelope().getPayload() instanceof ContractNegotiationFinalized) {
            return initiateTransfer((ContractNegotiationFinalized) message.getEventEnvelope().getPayload());
        }
        return Result.success();
    }

    private Result<Void> initiateTransfer(ContractNegotiationFinalized negotiationFinalized) {

        var transferRequest = TransferRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .contractId(negotiationFinalized.getContractAgreement().getId())
                .counterPartyAddress(negotiationFinalized.getCounterPartyAddress())
                .protocol(negotiationFinalized.getProtocol())
                .dataDestination(DATA_DESTINATION)
                .transferType(TRANSFER_TYPE)
                .callbackAddresses(negotiationFinalized.getCallbackAddresses())
                .build();

        var result = participantContextSupplier.get()
                .compose(participantContext -> transferProcessService.initiateTransfer(participantContext, transferRequest));

        if (result.failed()) {
            var msg = format("Failed to initiate a transfer for contract %s and asset %s, error: %s", negotiationFinalized.getContractAgreement().getId(), negotiationFinalized.getContractAgreement().getAssetId(), result.getFailureDetail());
            monitor.severe(msg);
            return Result.failure(msg);
        }
        monitor.debug(format("Transfer with id %s initiated", result.getContent()));
        return Result.success();
    }
}
