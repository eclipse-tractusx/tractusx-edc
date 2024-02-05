/********************************************************************************
 * Copyright (c) 2024 Robert Bosch Manufacturing Solutions GmbH and others
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol;

import org.assertj.core.api.Assertions;
import org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParamsProvider;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.web.spi.WebServer;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.WebServiceConfigurer;
import org.eclipse.edc.web.spi.configuration.WebServiceSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.DataPlaneHttpAccessControlExtension.ASPECT_MODEL_URL_PATTERN;
import static org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.DataPlaneHttpAccessControlExtension.DEFAULT_PORT;
import static org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.DataPlaneHttpAccessControlExtension.EDC_DTR_CONFIG_NAMES;
import static org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.DataPlaneHttpAccessControlExtension.EDC_DTR_CONFIG_PREFIX;
import static org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.DataPlaneHttpAccessControlExtension.ERROR_ENDPOINT_PORT;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
class DataPlaneHttpAccessControlExtensionTest {

    @Mock
    private WebService webService;
    @Mock
    private WebServer webServer;
    @Mock
    private WebServiceConfigurer configurer;
    @Mock
    private HttpRequestParamsProvider paramsProvider;
    @Mock
    private Monitor monitor;
    @Mock
    private Vault vault;
    @Mock
    private EdcHttpClient httpClient;

    private AutoCloseable openMocks;

    @BeforeEach
    void setUp(final ServiceExtensionContext context) {
        openMocks = MockitoAnnotations.openMocks(this);
        context.registerService(WebServer.class, webServer);
        context.registerService(WebService.class, webService);
        context.registerService(WebServiceConfigurer.class, configurer);
        context.registerService(HttpRequestParamsProvider.class, paramsProvider);
        context.registerService(Monitor.class, monitor);
        context.registerService(EdcHttpClient.class, httpClient);
        context.registerService(Vault.class, vault);
    }

    @AfterEach
    void tearDown() {
        Assertions.assertThatNoException().isThrownBy(() -> openMocks.close());
    }

    @Test
    void test_Initialize_ShouldRegisterComponents_WhenCalled(final DataPlaneHttpAccessControlExtension extension, final ServiceExtensionContext context) {
        //given
        doReturn("default").when(context)
                .getSetting(eq(EDC_DTR_CONFIG_NAMES), eq(""));
        doReturn("http://local-edc-wiremock:18080/aspect-model-api/").when(context)
                .getSetting(eq(EDC_DTR_CONFIG_PREFIX + "default" + ASPECT_MODEL_URL_PATTERN), anyString());
        doReturn(mock(Config.class)).when(context)
                .getConfig(EDC_DTR_CONFIG_PREFIX + "default");
        doReturn(DEFAULT_PORT).when(context).getSetting(eq(ERROR_ENDPOINT_PORT), anyInt());
        doAnswer(invocation -> {
            final WebServiceSettings settings = invocation.getArgument(2);
            ((WebServer) invocation.getArgument(1)).addPortMapping(settings.getName(), settings.getDefaultPort(), settings.getDefaultPath());
            return null;
        }).when(configurer).configure(same(context), any(), isA(WebServiceSettings.class));

        //when
        extension.initialize(context);

        //then
        verify(paramsProvider).registerSinkDecorator(isA(HttpAccessControlRequestParamsDecorator.class));
        verify(paramsProvider).registerSourceDecorator(isA(HttpAccessControlRequestParamsDecorator.class));
        verify(configurer).configure(same(context), same(webServer), isA(WebServiceSettings.class));
        verify(webServer).addPortMapping("error", DEFAULT_PORT, "/error");
        verify(webService).registerResource(eq("error"), isA(HttpAccessControlErrorApiController.class));
    }
}