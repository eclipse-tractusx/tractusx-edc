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

package org.eclipse.tractusx.edc.edr.core.manager;

import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequestData;
import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.TransferRequest;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.retry.ExponentialWaitStrategy;
import org.eclipse.edc.spi.retry.WaitStrategy;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.statemachine.StateMachineManager;
import org.eclipse.edc.statemachine.StateProcessorImpl;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessConfiguration;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessFactory;
import org.eclipse.tractusx.edc.edr.spi.EdrManager;
import org.eclipse.tractusx.edc.edr.spi.store.EndpointDataReferenceCache;
import org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntryStates;
import org.eclipse.tractusx.edc.edr.spi.types.NegotiateEdrRequest;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.eclipse.tractusx.edc.edr.core.EdrCoreExtension.DEFAULT_BATCH_SIZE;
import static org.eclipse.tractusx.edc.edr.core.EdrCoreExtension.DEFAULT_EXPIRED_RETENTION;
import static org.eclipse.tractusx.edc.edr.core.EdrCoreExtension.DEFAULT_EXPIRING_DURATION;
import static org.eclipse.tractusx.edc.edr.core.EdrCoreExtension.DEFAULT_ITERATION_WAIT;
import static org.eclipse.tractusx.edc.edr.core.EdrCoreExtension.DEFAULT_SEND_RETRY_BASE_DELAY;
import static org.eclipse.tractusx.edc.edr.core.EdrCoreExtension.DEFAULT_SEND_RETRY_LIMIT;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntryStates.EXPIRED;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntryStates.NEGOTIATED;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntryStates.from;

/**
 * Manages the EDR negotiation and lifecycle.
 **/
public class EdrManagerImpl implements EdrManager {

    public static final String LOCAL_ADAPTER_URI = "local://adapter";
    public static final Set<String> LOCAL_EVENTS = Set.of("contract.negotiation", "transfer.process");
    public static final CallbackAddress LOCAL_CALLBACK = CallbackAddress.Builder.newInstance()
            .transactional(true)
            .uri(LOCAL_ADAPTER_URI)
            .events(LOCAL_EVENTS)
            .build();
    protected Monitor monitor;
    protected ExecutorInstrumentation executorInstrumentation = ExecutorInstrumentation.noop();
    protected WaitStrategy waitStrategy = () -> DEFAULT_ITERATION_WAIT;

    protected int batchSize = DEFAULT_BATCH_SIZE;
    protected EntityRetryProcessFactory entityRetryProcessFactory;
    protected EntityRetryProcessConfiguration entityRetryProcessConfiguration = defaultEntityRetryProcessConfiguration();
    private ContractNegotiationService contractNegotiationService;

    private TransferProcessService transferProcessService;
    private StateMachineManager stateMachineManager;
    private EndpointDataReferenceCache edrCache;
    private Telemetry telemetry = new Telemetry();
    private Clock clock;
    private Duration expiringDuration;

    private Duration expiredRetention;


    private EdrManagerImpl() {
        expiringDuration = Duration.ofSeconds(DEFAULT_EXPIRING_DURATION);
        expiredRetention = Duration.ofSeconds(DEFAULT_EXPIRED_RETENTION);
    }

    @Override
    public StatusResult<ContractNegotiation> initiateEdrNegotiation(NegotiateEdrRequest request) {
        var negotiation = contractNegotiationService.initiateNegotiation(createContractRequest(request));
        return StatusResult.success(negotiation);
    }

    public void start() {
        stateMachineManager = StateMachineManager.Builder.newInstance("edr-manager", monitor, executorInstrumentation, waitStrategy)
                .processor(processEdrInState(NEGOTIATED, this::processNegotiated))
                .processor(processEdrInState(EXPIRED, this::processExpired))
                .build();

        stateMachineManager.start();
    }

    public void stop() {
        if (stateMachineManager != null) {
            stateMachineManager.stop();
        }
    }

    protected void transitionToRefreshing(EndpointDataReferenceEntry edrEntry) {
        edrEntry.transitionToRefreshing();
        update(edrEntry);
    }

    protected void transitionToNegotiated(EndpointDataReferenceEntry edrEntry) {
        edrEntry.transitionToNegotiated();
        update(edrEntry);
    }

    protected void transitionToExpired(EndpointDataReferenceEntry edrEntry) {
        edrEntry.transitionToExpired();
        update(edrEntry);
    }

    protected void transitionToDeleted(EndpointDataReferenceEntry edrEntry) {
        edrEntry.transitionToDeleted();
        update(edrEntry);
    }

    protected void transitionToError(EndpointDataReferenceEntry edrEntry, String message) {
        edrEntry.setErrorDetail(message);
        edrEntry.transitionError();
        update(edrEntry);
    }

    private void update(EndpointDataReferenceEntry edrEntry) {
        edrCache.update(edrEntry);
        monitor.debug(format("Edr entry %s is now in state %s.", edrEntry.getId(), from(edrEntry.getState())));
    }


    private StateProcessorImpl<EndpointDataReferenceEntry> processEdrInState(EndpointDataReferenceEntryStates state, Function<EndpointDataReferenceEntry, Boolean> function) {
        var filter = new Criterion[]{ hasState(state.code()) };
        return new StateProcessorImpl<>(() -> edrCache.nextNotLeased(batchSize, filter), telemetry.contextPropagationMiddleware(function));
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

    private boolean processNegotiated(EndpointDataReferenceEntry edrEntry) {
        if (isAboutToExpire(edrEntry)) {
            return entityRetryProcessFactory.doSyncProcess(edrEntry, () -> fireTransferProcess(edrEntry))
                    .onDelay(this::breakLease)
                    .onSuccess((n, result) -> transitionToRefreshing(n))
                    .onFailure((n, throwable) -> transitionToNegotiated(n))
                    .onFatalError((n, failure) -> transitionToError(n, failure.getFailureDetail()))
                    .onRetryExhausted((n, failure) -> transitionToError(n, format("Failed renew EDR token: %s", failure.getFailureDetail())))
                    .execute("Start an EDR token renewal");
        } else {
            breakLease(edrEntry);
            return false;
        }
    }

    private boolean processExpired(EndpointDataReferenceEntry edrEntry) {
        if (shouldBeRemoved(edrEntry)) {
            return entityRetryProcessFactory.doSyncProcess(edrEntry, () -> deleteEntry(edrEntry))
                    .onDelay(this::breakLease)
                    .onSuccess((n, result) -> {
                    })
                    .onFailure((n, throwable) -> transitionToExpired(n))
                    .onFatalError((n, failure) -> transitionToError(n, failure.getFailureDetail()))
                    .onRetryExhausted((n, failure) -> transitionToError(n, format("Failed delete EDR token: %s", failure.getFailureDetail())))
                    .execute("Start an EDR token deletion");
        } else {
            breakLease(edrEntry);
            return false;
        }
    }

    private StatusResult<Void> deleteEntry(EndpointDataReferenceEntry entry) {
        this.transitionToDeleted(entry);
        var result = edrCache.deleteByTransferProcessId(entry.getTransferProcessId());
        if (result.succeeded()) {
            monitor.debug(format("Deleted EDR cached entry for transfer process id %s", entry.getTransferProcessId()));
            return StatusResult.success();
        } else {
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, format("Failed to delete EDR for transfer process id %s, error: %s", entry.getTransferProcessId(), result.getFailureDetail()));
        }
    }

    private StatusResult<Void> fireTransferProcess(EndpointDataReferenceEntry entry) {

        var transferProcess = transferProcessService.findById(entry.getTransferProcessId());

        if (transferProcess == null) {
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, format("Failed to find transfer process %s", entry.getTransferProcessId()));
        }
        var dataRequest = transferProcess.getDataRequest();

        var newDataRequest = DataRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .assetId(dataRequest.getAssetId())
                .connectorId(dataRequest.getConnectorId())
                .contractId(dataRequest.getContractId())
                .protocol(dataRequest.getProtocol())
                .connectorAddress(dataRequest.getConnectorAddress())
                .dataDestination(dataRequest.getDataDestination())
                .destinationType(dataRequest.getDestinationType())
                .processId(dataRequest.getProcessId())
                .managedResources(dataRequest.isManagedResources())
                .build();

        var transferRequest = TransferRequest.Builder.newInstance()
                .dataRequest(newDataRequest)
                .callbackAddresses(transferProcess.getCallbackAddresses())
                .build();

        var result = transferProcessService.initiateTransfer(transferRequest);
        if (result.failed()) {
            var msg = format("Failed to initiate a transfer for contract %s and asset %s, error: %s", dataRequest.getContractId(), dataRequest.getAssetId(), result.getFailureDetail());
            monitor.severe(msg);
            return StatusResult.failure(ResponseStatus.ERROR_RETRY, result.getFailureDetail());
        }
        monitor.debug(format("Transfer with id %s initiated", result.getContent()));
        return StatusResult.success();
    }

    @NotNull
    private EntityRetryProcessConfiguration defaultEntityRetryProcessConfiguration() {
        return new EntityRetryProcessConfiguration(DEFAULT_SEND_RETRY_LIMIT, () -> new ExponentialWaitStrategy(DEFAULT_SEND_RETRY_BASE_DELAY));
    }

    private boolean isAboutToExpire(EndpointDataReferenceEntry entry) {
        if (entry.getExpirationTimestamp() == null) {
            return false;
        }
        var expiration = Instant.ofEpochMilli(entry.getExpirationTimestamp()).atOffset(ZoneOffset.UTC).toInstant();
        var now = clock.instant().atOffset(ZoneOffset.UTC).toInstant();
        var duration = Duration.between(now, expiration);
        return expiringDuration.compareTo(duration) > 0;
    }

    private boolean shouldBeRemoved(EndpointDataReferenceEntry entry) {
        if (entry.getExpirationTimestamp() == null) {
            return false;
        }
        var expiration = Instant.ofEpochMilli(entry.getExpirationTimestamp()).atOffset(ZoneOffset.UTC).toInstant();
        var now = clock.instant().atOffset(ZoneOffset.UTC).toInstant();
        var duration = Duration.between(now, expiration).abs();
        return expiredRetention.compareTo(duration) <= 0;
    }

    private void breakLease(EndpointDataReferenceEntry edrEntry) {
        edrCache.update(edrEntry);
    }

    public static class Builder {

        private final EdrManagerImpl edrManager;

        private Builder() {
            edrManager = new EdrManagerImpl();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder contractNegotiationService(ContractNegotiationService negotiationService) {
            edrManager.contractNegotiationService = negotiationService;
            return this;
        }

        public Builder transferProcessService(TransferProcessService transferProcessService) {
            edrManager.transferProcessService = transferProcessService;
            return this;
        }

        public Builder monitor(Monitor monitor) {
            edrManager.monitor = monitor;
            return this;
        }

        public Builder waitStrategy(WaitStrategy waitStrategy) {
            edrManager.waitStrategy = waitStrategy;
            return this;
        }

        public Builder executorInstrumentation(ExecutorInstrumentation executorInstrumentation) {
            edrManager.executorInstrumentation = executorInstrumentation;
            return this;
        }

        public Builder telemetry(Telemetry telemetry) {
            edrManager.telemetry = telemetry;
            return this;
        }

        public Builder clock(Clock clock) {
            edrManager.clock = clock;
            return this;
        }

        public Builder expiringDuration(Duration duration) {
            edrManager.expiringDuration = duration;
            return this;
        }

        public Builder expiredRetention(Duration duration) {
            edrManager.expiredRetention = duration;
            return this;
        }

        public Builder edrCache(EndpointDataReferenceCache edrCache) {
            edrManager.edrCache = edrCache;
            return this;
        }

        public Builder batchSize(int batchSize) {
            edrManager.batchSize = batchSize;
            return this;
        }

        public EdrManagerImpl build() {
            Objects.requireNonNull(edrManager.contractNegotiationService);
            Objects.requireNonNull(edrManager.monitor);
            Objects.requireNonNull(edrManager.waitStrategy);
            Objects.requireNonNull(edrManager.executorInstrumentation);
            Objects.requireNonNull(edrManager.edrCache);
            Objects.requireNonNull(edrManager.telemetry);
            Objects.requireNonNull(edrManager.transferProcessService);
            Objects.requireNonNull(edrManager.clock);
            Objects.requireNonNull(edrManager.expiringDuration);
            Objects.requireNonNull(edrManager.expiredRetention);

            edrManager.entityRetryProcessFactory = new EntityRetryProcessFactory(edrManager.monitor, edrManager.clock, edrManager.entityRetryProcessConfiguration);

            return edrManager;
        }
    }
}
