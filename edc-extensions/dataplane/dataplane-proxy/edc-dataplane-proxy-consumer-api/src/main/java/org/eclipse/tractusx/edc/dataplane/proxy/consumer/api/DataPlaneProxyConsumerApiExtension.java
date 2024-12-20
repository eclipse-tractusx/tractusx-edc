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
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebServer;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.WebServiceConfigurer;
import org.eclipse.edc.web.spi.configuration.WebServiceSettings;
import org.eclipse.tractusx.edc.dataplane.proxy.consumer.api.asset.ClientErrorExceptionMapper;
import org.eclipse.tractusx.edc.dataplane.proxy.consumer.api.asset.ConsumerAssetRequestController;
import org.eclipse.tractusx.edc.edr.spi.service.EdrService;

import java.util.UUID;
import java.util.concurrent.ExecutorService;

import static java.util.Optional.ofNullable;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.eclipse.tractusx.edc.core.utils.ConfigUtil.propertyCompatibility;

/**
 * Instantiates the Proxy Data API for the consumer-side data plane.
 */
@Extension(value = DataPlaneProxyConsumerApiExtension.NAME)
public class DataPlaneProxyConsumerApiExtension implements ServiceExtension {
    public static final int DEFAULT_THREAD_POOL = 10;
    @Setting("Vault alias for the Consumer Proxy API key")
    public static final String AUTH_SETTING_CONSUMER_PROXY_APIKEY_ALIAS = "tx.edc.dpf.consumer.proxy.auth.apikey.alias";
    @Setting("API key for the Consumer Proxy API")
    public static final String AUTH_SETTING_CONSUMER_PROXY_APIKEY = "tx.edc.dpf.consumer.proxy.auth.apikey";
    static final String NAME = "Data Plane Proxy Consumer API";
    private static final int DEFAULT_PROXY_PORT = 8186;
    private static final String CONSUMER_API_ALIAS = "consumer.api";
    private static final String CONSUMER_CONTEXT_PATH = "/proxy";
    private static final String CONSUMER_CONFIG_KEY = "web.http.proxy";
    @Setting(value = "Data plane proxy API consumer port", type = "int")
    private static final String CONSUMER_PORT = "tx.edc.dpf.consumer.proxy.port";
    @Deprecated(since = "0.7.1")
    private static final String CONSUMER_PORT_DEPRECATED = "tx.dpf.consumer.proxy.port";
    @Setting(value = "Thread pool size for the consumer data plane proxy gateway", type = "int")
    private static final String THREAD_POOL_SIZE = "tx.edc.dpf.consumer.proxy.thread.pool";
    @Deprecated(since = "0.7.1")
    private static final String THREAD_POOL_SIZE_DEPRECATED = "tx.dpf.consumer.proxy.thread.pool";
    @Deprecated(since = "0.7.1")
    private static final String AUTH_SETTING_APIKEY_ALIAS_DEPRECATED = "edc.api.auth.key.alias";
    @Deprecated(since = "0.7.1")
    private static final String AUTH_SETTING_APIKEY_DEPRECATED = "edc.api.auth.key";
    @Inject
    private WebService webService;

    @Inject
    private WebServer webServer;

    @Inject
    private PipelineService pipelineService;

    @Inject
    private EdrService edrService;

    @Inject
    private WebServiceConfigurer configurer;

    @Inject
    private Vault vault;

    @Inject
    private ApiAuthenticationRegistry apiAuthenticationRegistry;

    @Inject
    private Monitor monitor;

    private ExecutorService executorService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var port = propertyCompatibility(context, CONSUMER_PORT, CONSUMER_PORT_DEPRECATED, DEFAULT_PROXY_PORT);
        var config = context.getConfig(CONSUMER_CONFIG_KEY);

        configurer.configure(config, createApiContext(port));

        var poolSize = propertyCompatibility(context, THREAD_POOL_SIZE, THREAD_POOL_SIZE_DEPRECATED, DEFAULT_THREAD_POOL);
        executorService = newFixedThreadPool(poolSize);

        var authenticationService = createAuthenticationService(context);
        apiAuthenticationRegistry.register(CONSUMER_API_ALIAS, authenticationService);

        var authenticationFilter = new AuthenticationRequestFilter(apiAuthenticationRegistry, CONSUMER_API_ALIAS);
        webService.registerResource(CONSUMER_API_ALIAS, authenticationFilter);

        webService.registerResource(CONSUMER_API_ALIAS, new ClientErrorExceptionMapper());
        webService.registerResource(CONSUMER_API_ALIAS, new ConsumerAssetRequestController(edrService, pipelineService, executorService, monitor));
    }

    @Override
    public void shutdown() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    private AuthenticationService createAuthenticationService(ServiceExtensionContext context) {

        var apiKey = ofNullable(propertyCompatibility(context, AUTH_SETTING_CONSUMER_PROXY_APIKEY_ALIAS, AUTH_SETTING_APIKEY_ALIAS_DEPRECATED, null))
                .map(alias -> vault.resolveSecret(alias))
                .orElseGet(() -> propertyCompatibility(context, AUTH_SETTING_CONSUMER_PROXY_APIKEY, AUTH_SETTING_APIKEY_DEPRECATED, UUID.randomUUID().toString()));
        return new TokenBasedAuthenticationService(context.getMonitor().withPrefix("ConsumerProxyAPI"), apiKey);
    }

    private WebServiceSettings createApiContext(int port) {
        return WebServiceSettings.Builder.newInstance()
                .apiConfigKey(CONSUMER_CONFIG_KEY)
                .contextAlias(CONSUMER_API_ALIAS)
                .defaultPath(CONSUMER_CONTEXT_PATH)
                .defaultPort(port)
                .build();
    }

}
