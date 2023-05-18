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
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.tractusx.edc.spi.cp.adapter.model.NegotiateEdrRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.cp.adapter.callback.AdapterTransferProcessServiceImpl.LOCAL_CALLBACK;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AdapterTransferProcessServiceImplTest {

    ContractNegotiationService contractNegotiationService = mock(ContractNegotiationService.class);

    @Test
    void initEdrNegotiation_shouldFireAContractNegotiation_WhenUsingCallbacks() {
        var transferService = new AdapterTransferProcessServiceImpl(contractNegotiationService);

        var captor = ArgumentCaptor.forClass(ContractRequest.class);

        when(contractNegotiationService.initiateNegotiation(any())).thenReturn(getContractNegotiation());

        var negotiateEdrRequest = getNegotiateEdrRequest();

        var result = transferService.initiateEdrNegotiation(negotiateEdrRequest);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).isNotNull();

        verify(contractNegotiationService).initiateNegotiation(captor.capture());

        var msg = captor.getValue();

        assertThat(msg.getCallbackAddresses()).usingRecursiveFieldByFieldElementComparator().containsAll(negotiateEdrRequest.getCallbackAddresses());
        assertThat(msg.getCallbackAddresses()).usingRecursiveFieldByFieldElementComparator().contains(LOCAL_CALLBACK);
        assertThat(msg.getRequestData().getContractOffer()).usingRecursiveComparison().isEqualTo(negotiateEdrRequest.getOffer());
        assertThat(msg.getRequestData().getProtocol()).isEqualTo(negotiateEdrRequest.getProtocol());
        assertThat(msg.getRequestData().getCounterPartyAddress()).isEqualTo(negotiateEdrRequest.getConnectorAddress());

    }

    private NegotiateEdrRequest getNegotiateEdrRequest() {
        return NegotiateEdrRequest.Builder.newInstance()
                .protocol("protocol")
                .connectorAddress("http://test")
                .callbackAddresses(List.of(CallbackAddress.Builder.newInstance().uri("test").events(Set.of("test")).build()))
                .offer(ContractOffer.Builder.newInstance()
                        .id("id")
                        .assetId("assetId")
                        .policy(Policy.Builder.newInstance().build())
                        .providerId("provider")
                        .build())
                .build();
    }

    private ContractNegotiation getContractNegotiation() {
        return ContractNegotiation.Builder.newInstance()
                .id("id")
                .counterPartyAddress("http://test")
                .counterPartyId("provider")
                .protocol("protocol")
                .build();
    }
}
