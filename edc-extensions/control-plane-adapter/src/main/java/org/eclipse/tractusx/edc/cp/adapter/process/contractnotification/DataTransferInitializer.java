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

import lombok.RequiredArgsConstructor;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.TransferRequest;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.tractusx.edc.cp.adapter.dto.DataReferenceRetrievalDto;
import org.eclipse.tractusx.edc.cp.adapter.exception.ExternalRequestException;

@RequiredArgsConstructor
public class DataTransferInitializer {
    private final Monitor monitor;
    private final TransferProcessService transferProcessService;

    public String initiate(DataReferenceRetrievalDto dto) {
        monitor.info(
                String.format(
                        "[%s] ContractConfirmationHandler: transfer init - start.", dto.getTraceId()));
        DataAddress dataDestination = DataAddress.Builder.newInstance().type("HttpProxy").build();

        DataRequest dataRequest =
                DataRequest.Builder.newInstance()
                        .id(dto.getTraceId())
                        .assetId(dto.getPayload().getAssetId())
                        .contractId(dto.getPayload().getContractAgreementId())
                        .connectorId("provider")
                        .connectorAddress(dto.getPayload().getProvider())
                        .protocol("ids-multipart")
                        .dataDestination(dataDestination)
                        .managedResources(false)
                        .build();

        TransferRequest transferRequest = TransferRequest.Builder.newInstance().dataRequest(dataRequest).build();

        var result = transferProcessService.initiateTransfer(transferRequest);
        monitor.info(
                String.format("[%s] ContractConfirmationHandler: transfer init - end", dto.getTraceId()));
        if (result.failed()) {
            throwDataRefRequestException(dto);
        }

        return result.getContent().getId();
    }

    private void throwDataRefRequestException(DataReferenceRetrievalDto dto) {
        throw new ExternalRequestException(
                String.format(
                        "Data reference initial request failed! AssetId: %s", dto.getPayload().getAssetId()));
    }
}
