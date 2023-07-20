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

package org.eclipse.tractusx.edc.edr.core.service;

import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.service.spi.result.ServiceFailure;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.tractusx.edc.edr.spi.EdrManager;
import org.eclipse.tractusx.edc.edr.spi.store.EndpointDataReferenceCache;
import org.eclipse.tractusx.edc.edr.spi.types.NegotiateEdrRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EdrServiceImplTest {

    EdrManager edrManager = mock(EdrManager.class);

    EndpointDataReferenceCache endpointDataReferenceCache = mock(EndpointDataReferenceCache.class);

    EdrServiceImpl transferService;

    @BeforeEach
    void setup() {
        transferService = new EdrServiceImpl(edrManager, endpointDataReferenceCache);
    }

    @Test
    void initEdrNegotiation_shouldFireContractNegotiation_WhenUsingCallbacks() {

        when(edrManager.initiateEdrNegotiation(any())).thenReturn(StatusResult.success(getContractNegotiation()));

        var negotiateEdrRequest = getNegotiateEdrRequest();

        var result = transferService.initiateEdrNegotiation(negotiateEdrRequest);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).isNotNull();

    }

    @Test
    void findByTransferProcessId_shouldReturnTheEdr_whenFoundInCache() {

        var transferProcessId = "tpId";

        when(endpointDataReferenceCache.resolveReference(transferProcessId)).thenReturn(EndpointDataReference.Builder.newInstance().endpoint("test").build());

        var result = transferService.findByTransferProcessId(transferProcessId);

        assertThat(result)
                .isNotNull()
                .extracting(ServiceResult::getContent)
                .isNotNull();
    }


    @Test
    void findByTransferProcessId_shouldNotFound_whenNotPresentInCache() {
        var transferProcessId = "tpId";

        when(endpointDataReferenceCache.resolveReference(transferProcessId)).thenReturn(null);

        var result = transferService.findByTransferProcessId(transferProcessId);

        assertThat(result)
                .isNotNull()
                .extracting(ServiceResult::getFailure)
                .extracting(ServiceFailure::getReason)
                .isEqualTo(ServiceFailure.Reason.NOT_FOUND);
    }

    @Test
    void deleteByTransferProcessId() {
        var transferProcessId = "tpId";

        when(endpointDataReferenceCache.deleteByTransferProcessId(transferProcessId)).thenReturn(StoreResult.success(null));

        var result = transferService.deleteByTransferProcessId(transferProcessId);

        assertThat(result)
                .isNotNull()
                .extracting(ServiceResult::succeeded)
                .isEqualTo(true);
    }

    @Test
    void deleteByTransferProcessId_shouldNotFound_whenNotPresentInCache() {
        var transferProcessId = "tpId";

        when(endpointDataReferenceCache.deleteByTransferProcessId(transferProcessId)).thenReturn(StoreResult.notFound(""));

        var result = transferService.deleteByTransferProcessId(transferProcessId);

        assertThat(result)
                .isNotNull()
                .extracting(ServiceResult::getFailure)
                .extracting(ServiceFailure::getReason)
                .isEqualTo(ServiceFailure.Reason.NOT_FOUND);
    }

    @Test
    void queryEdrs() {
        when(endpointDataReferenceCache.queryForEntries(any())).thenReturn(Stream.empty());

        var result = transferService.findBy(QuerySpec.Builder.newInstance().build());

        assertThat(result)
                .isNotNull()
                .extracting(ServiceResult::getContent)
                .extracting(List::size)
                .isEqualTo(0);

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
