/********************************************************************************
 * Copyright (c) 2023 Mercedes Benz Tech Innovation GmbH
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

package org.eclipse.tractusx.edc.dataplane.proxy.provider.api.response;

import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.tractusx.edc.dataplane.proxy.provider.api.DataPlaneProxyProviderApiExtension;
import org.eclipse.tractusx.edc.dataplane.proxy.provider.api.gateway.ProviderGatewayController;
import org.eclipse.tractusx.edc.dataplane.proxy.provider.api.validation.ProxyProviderDataAddressResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

@ExtendWith(DependencyInjectionExtension.class)
class DataPlaneProxyProviderApiExtensionTest {

    private DataPlaneProxyProviderApiExtension extension;

    private static final String CONFIG_THREAD_POOL_SIZE_KEY = "tx.dpf.provider.proxy.thread.pool";
    private static final String CONFIG_THREAD_POOL_SIZE_VALUE = "10";

    private static final String CONFIG_WEB_HTTP_GATEWAY_PATH_KEY = "web.http.gateway.path";
    private static final String CONFIG_WEB_HTTP_GATEWAY_PATH_VALUE = "/api/v1/gateway";

    private static final String CONFIG_WEB_HTTP_GATEWAY_PORT_KEY = "web.http.gateway.port";
    private static final String CONFIG_WEB_HTTP_GATEWAY_PORT_VALUE = "11111";

    private static final String CONFIG_CONTROL_PLANE_VALIDATION_ENDPOINT_KEY = "edc.dataplane.token.validation.endpoint";
    private static final String CONFIG_CONTROL_PLANE_VALIDATION_ENDPOINT_VALUE = "http://example.com";

    // mocks
    private ServiceExtensionContext serviceExtensionContext;
    private ProviderGatewayController providerGatewayController;
    private Monitor monitor;
    private WebService webService;
    private ProxyProviderDataAddressResolver proxyProviderDataAddressResolver;
    private TypeManager typeManager;


    @BeforeEach
    void setup(ObjectFactory factory, ServiceExtensionContext context) {
        serviceExtensionContext = Mockito.mock(ServiceExtensionContext.class);
        providerGatewayController = Mockito.mock(ProviderGatewayController.class);
        monitor = Mockito.mock(Monitor.class);
        webService = Mockito.mock(WebService.class);
        proxyProviderDataAddressResolver = Mockito.mock(ProxyProviderDataAddressResolver.class);
        typeManager = Mockito.mock(TypeManager.class);

        Mockito.when(serviceExtensionContext.getService(ProviderGatewayController.class))
                .thenReturn(providerGatewayController);
        Mockito.when(serviceExtensionContext.getMonitor()).thenReturn(monitor);
        context.registerService(TypeManager.class, typeManager);
        context.registerService(WebService.class, webService);

        extension = factory.constructInstance(DataPlaneProxyProviderApiExtension.class);
    }

    private Map<String, String> getConfig() {
        return new HashMap<>() {
            {
                put(CONFIG_THREAD_POOL_SIZE_KEY, CONFIG_THREAD_POOL_SIZE_VALUE);
                put(CONFIG_CONTROL_PLANE_VALIDATION_ENDPOINT_KEY, CONFIG_CONTROL_PLANE_VALIDATION_ENDPOINT_VALUE);
            }
        };
    }

    private Map<String, String> getConfigWithContext() {
        var config = getConfig();
        config.put(CONFIG_WEB_HTTP_GATEWAY_PATH_KEY, CONFIG_WEB_HTTP_GATEWAY_PATH_VALUE);
        config.put(CONFIG_WEB_HTTP_GATEWAY_PORT_KEY, CONFIG_WEB_HTTP_GATEWAY_PORT_VALUE);
        return config;
    }

    @Test
    void testInitialize() {
        var config = ConfigFactory.fromMap(getConfig());
        Mockito.when(serviceExtensionContext.getConfig()).thenReturn(config);

        extension.initialize(serviceExtensionContext);

        Mockito.verify(webService, Mockito.times(1)).registerResource(Mockito.any(ProviderGatewayController.class));
    }

    @Test
    void testInitializeWithContext() {
        var config = ConfigFactory.fromMap(getConfigWithContext());
        Mockito.when(serviceExtensionContext.getConfig()).thenReturn(config);

        extension.initialize(serviceExtensionContext);

        Mockito.verify(webService, Mockito.times(1)).registerResource(Mockito.any(String.class), Mockito.any(ProviderGatewayController.class));
    }
}
