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
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.tractusx.edc.edr.spi.EdrManager;
import org.eclipse.tractusx.edc.edr.spi.service.EdrService;
import org.eclipse.tractusx.edc.edr.spi.store.EndpointDataReferenceCache;
import org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.eclipse.tractusx.edc.edr.spi.types.NegotiateEdrRequest;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class EdrServiceImpl implements EdrService {

    private final EdrManager edrManager;

    private final EndpointDataReferenceCache endpointDataReferenceCache;

    public EdrServiceImpl(EdrManager edrManager, EndpointDataReferenceCache endpointDataReferenceCache) {
        this.edrManager = edrManager;
        this.endpointDataReferenceCache = endpointDataReferenceCache;
    }

    @Override
    public ServiceResult<ContractNegotiation> initiateEdrNegotiation(NegotiateEdrRequest request) {
        var contractNegotiation = edrManager.initiateEdrNegotiation(request);
        if (contractNegotiation.succeeded()) {
            return ServiceResult.success(contractNegotiation.getContent());
        } else {
            return ServiceResult.badRequest(contractNegotiation.getFailureMessages());
        }
    }

    @Override
    public ServiceResult<EndpointDataReference> findByTransferProcessId(String transferProcessId) {
        var edr = endpointDataReferenceCache.resolveReference(transferProcessId);
        return Optional.ofNullable(edr)
                .map(ServiceResult::success)
                .orElse(ServiceResult.notFound(format("No Edr found associated to the transfer process with id: %s", transferProcessId)));
    }

    @Override
    public ServiceResult<List<EndpointDataReferenceEntry>> findBy(QuerySpec querySpec) {
        var results = endpointDataReferenceCache.queryForEntries(querySpec).collect(Collectors.toList());
        return ServiceResult.success(results);
    }

    @Override
    public ServiceResult<EndpointDataReferenceEntry> deleteByTransferProcessId(String transferProcessId) {
        var deleted = endpointDataReferenceCache.deleteByTransferProcessId(transferProcessId);
        return ServiceResult.from(deleted);
    }

}
