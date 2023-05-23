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
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.tractusx.edc.edr.spi.EndpointDataReferenceCache;
import org.eclipse.tractusx.edc.edr.spi.EndpointDataReferenceEntry;
import org.eclipse.tractusx.edc.spi.cp.adapter.model.NegotiateEdrRequest;
import org.eclipse.tractusx.edc.spi.cp.adapter.service.AdapterTransferProcessService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.eclipse.edc.service.spi.result.ServiceResult.notFound;
import static org.eclipse.edc.service.spi.result.ServiceResult.success;

public class AdapterTransferProcessServiceImpl implements AdapterTransferProcessService {

    public static final String LOCAL_ADAPTER_URI = "local://adapter";
    public static final Set<String> LOCAL_EVENTS = Set.of("contract.negotiation", "transfer.process");
    public static final CallbackAddress LOCAL_CALLBACK = CallbackAddress.Builder.newInstance()
            .transactional(true)
            .uri(LOCAL_ADAPTER_URI)
            .events(LOCAL_EVENTS)
            .build();
    private final ContractNegotiationService contractNegotiationService;

    private final EndpointDataReferenceCache endpointDataReferenceCache;

    public AdapterTransferProcessServiceImpl(ContractNegotiationService contractNegotiationService, EndpointDataReferenceCache endpointDataReferenceCache) {
        this.contractNegotiationService = contractNegotiationService;
        this.endpointDataReferenceCache = endpointDataReferenceCache;
    }

    @Override
    public ServiceResult<ContractNegotiation> initiateEdrNegotiation(NegotiateEdrRequest request) {
        var contractNegotiation = contractNegotiationService.initiateNegotiation(createContractRequest(request));
        return success(contractNegotiation);
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

    @Override
    public ServiceResult<EndpointDataReference> findByTransferProcessId(String transferProcessId) {
        var edr = endpointDataReferenceCache.resolveReference(transferProcessId);
        return Optional.ofNullable(edr)
                .map(ServiceResult::success)
                .orElse(notFound(format("No Edr found associated to the transfer process with id: %s", transferProcessId)));
    }

    @Override
    public ServiceResult<List<EndpointDataReferenceEntry>> findByAssetAndAgreement(String assetId, String agreementId) {
        var results = queryEdrs(assetId, agreementId)
                .stream()
                .filter(fieldFilter(assetId, EndpointDataReferenceEntry::getAssetId))
                .filter(fieldFilter(agreementId, EndpointDataReferenceEntry::getAgreementId))
                .collect(Collectors.toList());
        return success(results);
    }

    private Predicate<EndpointDataReferenceEntry> fieldFilter(String value, Function<EndpointDataReferenceEntry, String> function) {
        return entry -> Optional.ofNullable(value)
                .map(val -> val.equals(function.apply(entry)))
                .orElse(true);
    }

    private List<EndpointDataReferenceEntry> queryEdrs(String assetId, String agreementId) {
        // Try first for agreementId and then assetId
        return Optional.ofNullable(agreementId)
                .map(endpointDataReferenceCache::entriesForAgreement)
                .or(() -> Optional.ofNullable(assetId).map(endpointDataReferenceCache::entriesForAsset))
                .orElseGet(Collections::emptyList);
    }

}
