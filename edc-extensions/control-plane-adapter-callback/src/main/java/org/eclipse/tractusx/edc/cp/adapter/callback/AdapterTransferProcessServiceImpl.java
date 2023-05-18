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

package org.eclipse.tractusx.edc.cp.adapter.callback;

import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequestData;
import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.tractusx.edc.spi.cp.adapter.model.NegotiateEdrRequest;
import org.eclipse.tractusx.edc.spi.cp.adapter.service.AdapterTransferProcessService;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AdapterTransferProcessServiceImpl implements AdapterTransferProcessService {

    public static final String LOCAL_ADAPTER_URI = "local://adapter";
    public static final Set<String> LOCAL_EVENTS = Set.of("contract.negotiation", "transfer.process");
    public static final CallbackAddress LOCAL_CALLBACK = CallbackAddress.Builder.newInstance()
            .transactional(true)
            .uri(LOCAL_ADAPTER_URI)
            .events(LOCAL_EVENTS)
            .build();
    private final ContractNegotiationService contractNegotiationService;

    public AdapterTransferProcessServiceImpl(ContractNegotiationService contractNegotiationService) {
        this.contractNegotiationService = contractNegotiationService;
    }

    @Override
    public ServiceResult<ContractNegotiation> initiateEdrNegotiation(NegotiateEdrRequest request) {
        var contractNegotiation = contractNegotiationService.initiateNegotiation(createContractRequest(request));
        return ServiceResult.success(contractNegotiation);
    }

    private ContractRequest createContractRequest(NegotiateEdrRequest request) {
        var callbacks = Stream.concat(request.getCallbackAddresses().stream(), Stream.of(LOCAL_CALLBACK)).collect(Collectors.toList());

        var requestData = ContractRequestData.Builder.newInstance()
                .contractOffer(request.getOffer())
                .protocol(request.getProtocol())
                .counterPartyAddress(request.getConnectorAddress())
                .connectorId(request.getConnectorId())
                .build();

        return ContractRequest.Builder.newInstance()
                .requestData(requestData)
                .callbackAddresses(callbacks).build();
    }
}
