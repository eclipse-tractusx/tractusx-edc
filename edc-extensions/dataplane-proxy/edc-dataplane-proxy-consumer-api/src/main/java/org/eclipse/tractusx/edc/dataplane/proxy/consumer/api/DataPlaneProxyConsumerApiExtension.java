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

package org.eclipse.tractusx.edc.dataplane.proxy.consumer.api;

import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebServer;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.WebServiceConfigurer;
import org.eclipse.edc.web.spi.configuration.WebServiceSettings;
import org.eclipse.tractusx.edc.dataplane.proxy.consumer.api.asset.ClientErrorExceptionMapper;
import org.eclipse.tractusx.edc.dataplane.proxy.consumer.api.asset.ConsumerAssetRequestController;
import org.eclipse.tractusx.edc.edr.spi.store.EndpointDataReferenceCache;

import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newFixedThreadPool;

/**
 * Instantiates the Proxy Data API for the consumer-side data plane.
 */
@Extension(value = DataPlaneProxyConsumerApiExtension.NAME)
public class DataPlaneProxyConsumerApiExtension implements ServiceExtension {
    public static final int DEFAULT_THREAD_POOL = 10;
    static final String NAME = "Data Plane Proxy Consumer API";
    private static final int DEFAULT_PROXY_PORT = 8186;
    private static final String CONSUMER_API_ALIAS = "consumer.api";
    private static final String CONSUMER_CONTEXT_PATH = "/proxy";
    private static final String CONSUMER_CONFIG_KEY = "web.http.proxy";
    @Setting(value = "Data plane proxy API consumer port", type = "int")
    private static final String CONSUMER_PORT = "tx.dpf.consumer.proxy.port";
    @Setting(value = "Thread pool size for the consumer data plane proxy gateway", type = "int")
    private static final String THREAD_POOL_SIZE = "tx.dpf.consumer.proxy.thread.pool";
    @Inject
    private WebService webService;

    @Inject
    private WebServer webServer;

    @Inject
    private DataPlaneManager dataPlaneManager;

    @Inject
    private EndpointDataReferenceCache edrCache;

    @Inject
    private WebServiceConfigurer configurer;

    @Inject
    private Monitor monitor;

    private ExecutorService executorService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var port = context.getSetting(CONSUMER_PORT, DEFAULT_PROXY_PORT);
        configurer.configure(context, webServer, createApiContext(port));

        executorService = newFixedThreadPool(context.getSetting(THREAD_POOL_SIZE, DEFAULT_THREAD_POOL));

        webService.registerResource(CONSUMER_API_ALIAS, new ClientErrorExceptionMapper());
        webService.registerResource(CONSUMER_API_ALIAS, new ConsumerAssetRequestController(edrCache, dataPlaneManager, executorService, monitor));
    }

    @Override
    public void shutdown() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    private WebServiceSettings createApiContext(int port) {
        return WebServiceSettings.Builder.newInstance()
                .apiConfigKey(CONSUMER_CONFIG_KEY)
                .contextAlias(CONSUMER_API_ALIAS)
                .defaultPath(CONSUMER_CONTEXT_PATH)
                .defaultPort(port)
                .name(NAME)
                .build();
    }

}
