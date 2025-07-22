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
import org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParams;
import org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParamsProvider;
import org.eclipse.edc.connector.dataplane.http.testfixtures.TestFunctions;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.spi.types.domain.transfer.FlowType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProxyHttpDataSourceFactoryTest {

    private final EdcHttpClient httpClient = mock();
    private final Monitor monitor = mock();
    private final HttpRequestParamsProvider provider = mock();
    private final HttpRequestFactory requestFactory = mock();

    private ProxyHttpDataSourceFactory factory;

    @BeforeEach
    void setUp() {
        factory = new ProxyHttpDataSourceFactory(httpClient, provider, monitor, requestFactory);
    }

    @Test
    void verifyValidationFailsIfSupplierThrows() {
        var errorMsg = "Test error message";
        var request = createRequest(DataAddress.Builder.newInstance().type("Test type").build(), FlowType.PULL);

        when(provider.provideSourceParams(request)).thenThrow(new EdcException(errorMsg));

        var result = factory.validateRequest(request);
        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).hasSize(1);
        assertThat(result.getFailureMessages().get(0)).contains(errorMsg);
    }

    @Test
    void verifySuccessSourceCreation() {
        var address = HttpDataAddress.Builder.newInstance()
                .name("test address name")
                .build();
        var request = createRequest(address, FlowType.PULL);
        var params = mock(HttpRequestParams.class);

        when(provider.provideSourceParams(request)).thenReturn(params);

        assertThat(factory.validateRequest(request).succeeded()).isTrue();
        var source = factory.createSource(request);
        assertThat(source).isNotNull();

        var expected = ProxyHttpDataSource.Builder.newInstance()
                .params(params)
                .name(address.getName())
                .requestId(request.getId())
                .httpClient(httpClient)
                .monitor(monitor)
                .requestFactory(requestFactory)
                .build();

        assertThat(source).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void verifyFailureSourceCreationWithPullTransferType() {
        var address = HttpDataAddress.Builder.newInstance()
                .name("test address name")
                .build();
        var request = createRequest(address, FlowType.PUSH);
        var params = mock(HttpRequestParams.class);

        when(provider.provideSourceParams(request)).thenReturn(params);

        assertThat(factory.validateRequest(request).failed()).isTrue();
    }

    private DataFlowStartMessage createRequest(DataAddress source, FlowType flowType) {
        return TestFunctions.createRequest(
                        emptyMap(),
                        source,
                        DataAddress.Builder.newInstance().type("Test type").build(),
                        flowType)
                .build();
    }
}