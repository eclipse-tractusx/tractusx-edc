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

package org.eclipse.tractusx.edc.dataplane.http.pipeline;

import org.eclipse.edc.connector.dataplane.http.params.HttpRequestFactory;
import org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress;
import org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParamsProvider;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.jetbrains.annotations.NotNull;

/**
 * Instantiates {@link ProxyHttpDataSource}s for requests whose source data type is ProxyHttpData.
 */
public class ProxyHttpDataSourceFactory implements DataSourceFactory {

    private final EdcHttpClient httpClient;
    private final HttpRequestParamsProvider requestParamsProvider;
    private final Monitor monitor;
    private final HttpRequestFactory requestFactory;

    public ProxyHttpDataSourceFactory(EdcHttpClient httpClient, HttpRequestParamsProvider requestParamsProvider, Monitor monitor, HttpRequestFactory requestFactory) {
        this.httpClient = httpClient;
        this.requestParamsProvider = requestParamsProvider;
        this.monitor = monitor;
        this.requestFactory = requestFactory;
    }

    @Override
    public String supportedType() {
        return "ProxyHttpData";
    }

    @Override
    public @NotNull Result<Void> validateRequest(DataFlowStartMessage request) {
        try {
            createSource(request);
        } catch (Exception e) {
            return Result.failure("Failed to build HttpDataSource: " + e.getMessage());
        }
        return Result.success();
    }

    @Override
    public DataSource createSource(DataFlowStartMessage request) {
        var dataAddress = HttpDataAddress.Builder.newInstance()
                .copyFrom(request.getSourceDataAddress())
                .build();
        return ProxyHttpDataSource.Builder.newInstance()
                .httpClient(httpClient)
                .monitor(monitor)
                .requestId(request.getId())
                .name(dataAddress.getName())
                .params(requestParamsProvider.provideSourceParams(request))
                .requestFactory(requestFactory)
                .build();
    }
}
