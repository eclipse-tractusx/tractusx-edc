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

package org.eclipse.tractusx.edc.edr.core;

import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.retry.ExponentialWaitStrategy;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.tractusx.edc.edr.core.manager.EdrManagerImpl;
import org.eclipse.tractusx.edc.edr.spi.EdrManager;
import org.eclipse.tractusx.edc.edr.spi.store.EndpointDataReferenceCache;

import java.time.Clock;
import java.time.Duration;

/**
 * Registers default services for the EDR cache.
 */
@Provides(EdrManager.class)
@Extension(value = EdrCoreExtension.NAME)
public class EdrCoreExtension implements ServiceExtension {
    public static final long DEFAULT_ITERATION_WAIT = 1000;

    public static final int DEFAULT_BATCH_SIZE = 20;

    public static final int DEFAULT_SEND_RETRY_LIMIT = 7;

    public static final long DEFAULT_SEND_RETRY_BASE_DELAY = 1000L;
    public static final long DEFAULT_EXPIRING_DURATION = 60;

    public static final long DEFAULT_EXPIRED_RETENTION = 60;

    protected static final String NAME = "EDR Core";
    @Setting(value = "The iteration wait time in milliseconds in the edr state machine. Default value " + DEFAULT_ITERATION_WAIT, type = "long")
    private static final String EDR_STATE_MACHINE_ITERATION_WAIT_MILLIS = "edc.edr.state-machine.iteration-wait-millis";
    @Setting(value = "The batch size in the edr negotiation state machine. Default value " + DEFAULT_BATCH_SIZE, type = "int")
    private static final String EDR_STATE_MACHINE_BATCH_SIZE = "edc.edr.state-machine.batch-size";
    @Setting(value = "The minimum duration on which the EDR token can be eligible for renewal. Default value " + DEFAULT_EXPIRING_DURATION + " (seconds)", type = "long")
    private static final String EDR_STATE_MACHINE_EXPIRING_DURATION = "edc.edr.state-machine.expiring-duration";

    @Setting(value = "The minimum duration on with the EDR token can be eligible for deletion when it's expired. Default value " + DEFAULT_EXPIRED_RETENTION + " (seconds)", type = "long")
    private static final String EDR_STATE_MACHINE_EXPIRED_RETENTION = "edc.edr.state-machine.expired-retention";

    @Inject
    private Monitor monitor;

    @Inject
    private ContractNegotiationService contractNegotiationService;

    @Inject
    private TransferProcessService transferProcessService;
    @Inject
    private EndpointDataReferenceCache endpointDataReferenceCache;

    @Inject
    private ExecutorInstrumentation executorInstrumentation;

    @Inject
    private Telemetry telemetry;

    @Inject
    private Clock clock;
    private EdrManagerImpl edrManager;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {

        var iterationWaitMillis = context.getSetting(EDR_STATE_MACHINE_ITERATION_WAIT_MILLIS, DEFAULT_ITERATION_WAIT);

        var expiringDuration = context.getSetting(EDR_STATE_MACHINE_EXPIRING_DURATION, DEFAULT_EXPIRING_DURATION);

        var expiredRetention = context.getSetting(EDR_STATE_MACHINE_EXPIRED_RETENTION, DEFAULT_EXPIRED_RETENTION);


        edrManager = EdrManagerImpl.Builder.newInstance()
                .contractNegotiationService(contractNegotiationService)
                .monitor(monitor)
                .waitStrategy(new ExponentialWaitStrategy(iterationWaitMillis))
                .executorInstrumentation(executorInstrumentation)
                .edrCache(endpointDataReferenceCache)
                .transferProcessService(transferProcessService)
                .telemetry(telemetry)
                .batchSize(context.getSetting(EDR_STATE_MACHINE_BATCH_SIZE, DEFAULT_BATCH_SIZE))
                .expiringDuration(Duration.ofSeconds(expiringDuration))
                .expiredRetention(Duration.ofSeconds(expiredRetention))
                .clock(clock)
                .build();

        context.registerService(EdrManager.class, edrManager);
    }

    @Override
    public void start() {
        edrManager.start();
    }

    @Override
    public void shutdown() {
        if (edrManager != null) {
            edrManager.stop();
        }
    }

}
