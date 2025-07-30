/********************************************************************************
 * Copyright (c) 2025 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.dataplane.http;

import org.eclipse.edc.connector.dataplane.http.params.HttpRequestFactory;
import org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParamsProvider;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.tractusx.edc.dataplane.http.pipeline.ProxyHttpDataSourceFactory;

/**
 * Provides support for reading data from a Proxy HTTP.
 */
@Extension(value = DataPlaneProxyHttpExtension.NAME)
public class DataPlaneProxyHttpExtension implements ServiceExtension {
    protected static final String NAME = "Data Plane Proxy Http Extension";

    @Inject
    private PipelineService pipelineService;
    @Inject
    private EdcHttpClient httpClient;
    @Inject
    private HttpRequestParamsProvider paramsProvider;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        var httpRequestFactory = new HttpRequestFactory();

        var sourceFactory = new ProxyHttpDataSourceFactory(httpClient, paramsProvider, monitor, httpRequestFactory);
        pipelineService.registerFactory(sourceFactory);
    }
}
