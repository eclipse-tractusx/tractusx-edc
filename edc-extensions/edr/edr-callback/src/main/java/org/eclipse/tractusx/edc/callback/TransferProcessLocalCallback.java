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

import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.connector.spi.callback.CallbackEventRemoteMessage;
import org.eclipse.edc.connector.transfer.spi.event.TransferProcessStarted;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.tractusx.edc.edr.spi.store.EndpointDataReferenceCache;
import org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntryStates;
import org.eclipse.tractusx.edc.spi.callback.InProcessCallback;

import java.text.ParseException;
import java.time.ZoneOffset;

import static java.lang.String.format;

public class TransferProcessLocalCallback implements InProcessCallback {

    private final EndpointDataReferenceCache edrCache;
    private final TransferProcessStore transferProcessStore;
    private final TypeTransformerRegistry transformerRegistry;

    private final TransactionContext transactionContext;

    private final Monitor monitor;

    public TransferProcessLocalCallback(EndpointDataReferenceCache edrCache, TransferProcessStore transferProcessStore, TypeTransformerRegistry transformerRegistry, TransactionContext transactionContext, Monitor monitor) {
        this.edrCache = edrCache;
        this.transferProcessStore = transferProcessStore;
        this.transformerRegistry = transformerRegistry;
        this.transactionContext = transactionContext;
        this.monitor = monitor;
    }

    @Override
    public <T extends Event> Result<Void> invoke(CallbackEventRemoteMessage<T> message) {
        if (message.getEventEnvelope().getPayload() instanceof TransferProcessStarted transferProcessStarted) {
            if (transferProcessStarted.getDataAddress() != null) {
                return transformerRegistry.transform(transferProcessStarted.getDataAddress(), EndpointDataReference.class)
                        .compose(this::storeEdr)
                        .mapTo();
            }
        }
        return Result.success();
    }

    private Result<Void> storeEdr(EndpointDataReference edr) {
        return transactionContext.execute(() -> {
            var transferProcess = transferProcessStore.findForCorrelationId(edr.getId());
            if (transferProcess != null) {
                var expirationTime = extractExpirationTime(edr);

                if (expirationTime.failed()) {
                    return expirationTime.mapTo();
                }
                var cacheEntry = EndpointDataReferenceEntry.Builder.newInstance()
                        .transferProcessId(transferProcess.getId())
                        .assetId(transferProcess.getDataRequest().getAssetId())
                        .agreementId(transferProcess.getDataRequest().getContractId())
                        .providerId(transferProcess.getDataRequest().getConnectorId())
                        .state(EndpointDataReferenceEntryStates.NEGOTIATED.code())
                        .expirationTimestamp(expirationTime.getContent())
                        .build();

                cleanOldEdr(transferProcess.getDataRequest().getAssetId(), transferProcess.getDataRequest().getContractId());
                edrCache.save(cacheEntry, edr);

                return Result.success();
            } else {
                return Result.failure(format("Failed to find a transfer process with correlation ID %s", edr.getId()));
            }
        });

    }

    private void cleanOldEdr(String assetId, String agreementId) {
        var querySpec = QuerySpec.Builder.newInstance()
                .filter(fieldFilter("agreementId", agreementId))
                .filter(fieldFilter("assetId", assetId))
                .build();

        edrCache.queryForEntries(querySpec).forEach((entry -> {
            monitor.debug(format("Expiring EDR for transfer process %s", entry.getTransferProcessId()));
            entry.transitionToExpired();
            edrCache.update(entry);
        }));
    }

    private Result<Long> extractExpirationTime(EndpointDataReference edr) {
        try {
            if (edr.getAuthCode() != null) {
                var jwt = SignedJWT.parse(edr.getAuthCode());
                var expirationTime = jwt.getJWTClaimsSet().getExpirationTime();
                if (expirationTime != null) {
                    return Result.success(expirationTime
                            .toInstant()
                            .atOffset(ZoneOffset.UTC)
                            .toInstant().toEpochMilli());
                }
            }
        } catch (ParseException e) {
            return Result.failure(format("Failed to parts JWT token for edr %s", edr.getId()));
        }
        return Result.success(0L);
    }

    private Criterion fieldFilter(String field, String value) {
        return Criterion.Builder.newInstance()
                .operandLeft(field)
                .operator("=")
                .operandRight(value)
                .build();
    }
}
