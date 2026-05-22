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

package org.eclipse.tractusx.edc.dataplane.proxy.consumer.api;

import org.eclipse.edc.api.auth.spi.AuthenticationRequestFilter;
import org.eclipse.edc.api.auth.spi.AuthenticationService;
import org.eclipse.edc.api.auth.spi.registry.ApiAuthenticationRegistry;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.PortMapping;
import org.eclipse.edc.web.spi.configuration.PortMappingRegistry;
import org.eclipse.tractusx.edc.dataplane.proxy.consumer.api.asset.ClientErrorExceptionMapper;
import org.eclipse.tractusx.edc.dataplane.proxy.consumer.api.asset.ConsumerAssetRequestController;
import org.eclipse.tractusx.edc.edr.spi.service.EdrService;

import java.util.UUID;
import java.util.concurrent.ExecutorService;

import static java.util.Optional.ofNullable;
import static java.util.concurrent.Executors.newFixedThreadPool;

/**
 * Instantiates the Proxy Data API for the consumer-side data plane.
 */
@Extension(value = DataPlaneProxyConsumerApiExtension.NAME)
public class DataPlaneProxyConsumerApiExtension implements ServiceExtension {

    public static final String NAME = "Data Plane Proxy Consumer API";
    private static final String PROXY = "proxy";
    private static final int DEFAULT_PROXY_PORT = 8186;
    private static final String DEFAULT_PROXY_PATH = "/proxy";
    private static final int DEFAULT_THREAD_POOL = 10;
    private static final String DEFAULT_API_KEY = UUID.randomUUID().toString();

    private static final String CONSUMER_PORT = "tx.edc.dpf.consumer.proxy.port";
    private static final String THREAD_POOL_SIZE = "tx.edc.dpf.consumer.proxy.thread.pool";
    private static final String AUTH_SETTING_CONSUMER_PROXY_APIKEY = "tx.edc.dpf.consumer.proxy.auth.apikey";
    private static final String AUTH_SETTING_CONSUMER_PROXY_APIKEY_ALIAS = "tx.edc.dpf.consumer.proxy.auth.apikey.alias";

    @Setting(key = AUTH_SETTING_CONSUMER_PROXY_APIKEY_ALIAS, description = "Vault alias for the Consumer Proxy API key", required = false)
    private String apiKeyAlias;

    @Setting(key = AUTH_SETTING_CONSUMER_PROXY_APIKEY, description = "API key for the Consumer Proxy API", required = false)
    private String configuredApiKey;

    @Setting(key = CONSUMER_PORT, description = "Data plane proxy API consumer port", defaultValue = DEFAULT_PROXY_PORT + "")
    private int port;

    @Setting(key = THREAD_POOL_SIZE, description = "Thread pool size for the consumer data plane proxy gateway", defaultValue = DEFAULT_THREAD_POOL + "")
    private int poolSize;

    @Configuration
    private DataPlaneProxyConsumerApiConfiguration apiConfiguration;

    @Inject
    private WebService webService;
    @Inject
    private PipelineService pipelineService;
    @Inject
    private EdrService edrService;
    @Inject
    private Vault vault;
    @Inject
    private ApiAuthenticationRegistry apiAuthenticationRegistry;
    @Inject
    private Monitor monitor;
    @Inject
    private PortMappingRegistry portMappingRegistry;

    private ExecutorService executorService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        // when deprecated port will be purged, just assign `apiConfiguration.port()` to `port`
        var portMapping = new PortMapping(PROXY, port, apiConfiguration.path());
        portMappingRegistry.register(portMapping);

        executorService = newFixedThreadPool(poolSize);

        var authenticationService = createAuthenticationService(context);
        apiAuthenticationRegistry.register(PROXY, authenticationService);

        var authenticationFilter = new AuthenticationRequestFilter(apiAuthenticationRegistry, PROXY);
        webService.registerResource(PROXY, authenticationFilter);

        webService.registerResource(PROXY, new ClientErrorExceptionMapper());
        webService.registerResource(PROXY, new ConsumerAssetRequestController(edrService, pipelineService, executorService, monitor));
    }

    @Override
    public void shutdown() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    private AuthenticationService createAuthenticationService(ServiceExtensionContext context) {
        var apiKey = DEFAULT_API_KEY;
        if (apiKeyAlias != null) {
            apiKey = vault.resolveSecret(apiKeyAlias);
        } else if (configuredApiKey != null) {
            apiKey = configuredApiKey;
        }
        return new TokenBasedAuthenticationService(context.getMonitor().withPrefix("ConsumerProxyAPI"), apiKey);
    }

    @Settings
    record DataPlaneProxyConsumerApiConfiguration(
            @Setting(key = "web.http." + PROXY + ".port", description = "Port for " + PROXY + " api context", defaultValue = DEFAULT_PROXY_PORT + "")
            int port,
            @Setting(key = "web.http." + PROXY + ".path", description = "Path for " + PROXY + " api context", defaultValue = DEFAULT_PROXY_PATH)
            String path
    ) {

    }

}
