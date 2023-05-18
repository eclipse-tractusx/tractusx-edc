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

package org.eclipse.tractusx.edc.dataplane.proxy.provider.api;

import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.tractusx.edc.dataplane.proxy.provider.api.gateway.ProviderGatewayController;
import org.eclipse.tractusx.edc.dataplane.proxy.spi.provider.gateway.authorization.AuthorizationHandlerRegistry;
import org.eclipse.tractusx.edc.dataplane.proxy.spi.provider.gateway.configuration.GatewayConfigurationRegistry;

import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newFixedThreadPool;

/**
 * Adds the consumer proxy data plane API.
 */
@Extension(value = DataPlaneProxyProviderApiExtension.NAME)
public class DataPlaneProxyProviderApiExtension implements ServiceExtension {
    static final String NAME = "Data Plane Proxy Provider API";

    @Setting(value = "Thread pool size for the provider data plane proxy gateway", type = "int")
    private static final String THREAD_POOL_SIZE = "tx.dpf.provider.proxy.thread.pool";

    public static final int DEFAULT_THREAD_POOL = 10;

    @Inject
    private WebService webService;

    @Inject
    private DataPlaneManager dataPlaneManager;

    @Inject
    private Monitor monitor;

    @Inject
    private GatewayConfigurationRegistry configurationRegistry;

    @Inject
    private AuthorizationHandlerRegistry authorizationRegistry;

    private ExecutorService executorService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        executorService = newFixedThreadPool(context.getSetting(THREAD_POOL_SIZE, DEFAULT_THREAD_POOL));

        var controller = new ProviderGatewayController(dataPlaneManager,
                configurationRegistry,
                authorizationRegistry,
                executorService,
                monitor);

        webService.registerResource(controller);
    }


    @Override
    public void shutdown() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }

}
