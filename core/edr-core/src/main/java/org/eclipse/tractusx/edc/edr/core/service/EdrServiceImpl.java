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

package org.eclipse.tractusx.edc.edr.core.service;

import org.eclipse.edc.edr.spi.store.EndpointDataReferenceStore;
import org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.tractusx.edc.edr.spi.index.lock.EndpointDataReferenceLock;
import org.eclipse.tractusx.edc.edr.spi.service.EdrService;
import org.eclipse.tractusx.edc.edr.spi.types.RefreshMode;
import org.eclipse.tractusx.edc.spi.tokenrefresh.common.TokenRefreshHandler;

import java.util.List;

public class EdrServiceImpl implements EdrService {

    private final EndpointDataReferenceStore edrStore;
    private final TokenRefreshHandler tokenRefreshHandler;
    private final TransactionContext transactionContext;
    private final Monitor monitor;
    private final EndpointDataReferenceLock edrLock;

    public EdrServiceImpl(EndpointDataReferenceStore edrStore, TokenRefreshHandler tokenRefreshHandler, TransactionContext transactionContext, Monitor monitor, EndpointDataReferenceLock edrLock) {
        this.edrStore = edrStore;
        this.tokenRefreshHandler = tokenRefreshHandler;
        this.transactionContext = transactionContext;
        this.monitor = monitor.withPrefix(getClass().getSimpleName());
        this.edrLock = edrLock;
    }

    @Override
    public ServiceResult<DataAddress> resolveByTransferProcess(String transferProcessId, RefreshMode mode) {
        return transactionContext.execute(() -> edrStore.resolveByTransferProcess(transferProcessId)
                .flatMap(ServiceResult::from)
                .compose(edr -> handleRefresh(transferProcessId, edr, mode)));

    }

    @Override
    public ServiceResult<List<EndpointDataReferenceEntry>> query(QuerySpec query) {
        return transactionContext.execute(() -> edrStore.query(query).flatMap(ServiceResult::from));
    }

    private ServiceResult<DataAddress> handleRefresh(String id, DataAddress edr, RefreshMode mode) {
        return switch (mode) {
            case NO_REFRESH -> ServiceResult.success(edr);
            case AUTO_REFRESH, FORCE_REFRESH -> autoRefresh(id, edr, mode);
        };
    }

    private ServiceResult<DataAddress> autoRefresh(String id, DataAddress edr, RefreshMode mode) {
        var edrEntry = edrStore.findById(id);
        if (edrEntry == null) {
            var msg = "An EndpointDataReferenceEntry with ID '%s' does not exist".formatted(id);
            monitor.warning(msg);
            return ServiceResult.notFound(msg);
        }
        if (edrLock.isExpired(edr, edrEntry) || mode.equals(RefreshMode.FORCE_REFRESH)) {
            var result = ServiceResult.from(edrLock.acquireLock(id, edr))
                    .compose(shouldRefresh -> {
                        if (!shouldRefresh && !mode.equals(RefreshMode.FORCE_REFRESH)) {
                            monitor.debug("Don't need to refresh. Will resolve existing.");
                            var refreshedEdr = edrStore.resolveByTransferProcess(id);
                            return ServiceResult.from(refreshedEdr);
                        } else {
                            monitor.debug("Token '%s' expired, need to refresh.".formatted(id));
                            return tokenRefreshHandler.refreshToken(id, edr)
                                    .compose(updated -> updateEdr(edrEntry, updated));
                        }
                    });
            edrLock.releaseLock(id)
                    .onFailure(error -> monitor.warning("Error releasing lock: %s".formatted(error)));
            return result;

        }
        var refreshedEdr = edrStore.resolveByTransferProcess(id);
        return ServiceResult.from(refreshedEdr);
    }

    private ServiceResult<DataAddress> updateEdr(EndpointDataReferenceEntry entry, DataAddress dataAddress) {
        var newEntry = EndpointDataReferenceEntry.Builder.newInstance()
                .assetId(entry.getAssetId())
                .agreementId(entry.getAgreementId())
                .providerId(entry.getProviderId())
                .transferProcessId(entry.getTransferProcessId())
                .contractNegotiationId(entry.getContractNegotiationId())
                .agreementId(entry.getAgreementId())
                .build();

        var updateResult = edrStore.save(newEntry, dataAddress);


        if (updateResult.failed()) {
            return ServiceResult.fromFailure(updateResult);
        }
        return ServiceResult.success(dataAddress);
    }


}
