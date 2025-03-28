/**
 * Copyright (c) 2022 Amadeus - initial API and implementation
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
 **/

package org.eclipse.tractusx.edc.dataplane.proxy.api.controller;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.connector.dataplane.spi.schema.DataFlowRequestSchema;
import org.eclipse.edc.connector.dataplane.util.sink.AsyncStreamingDataSink;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataFlowStartMessageSupplierTest {


    private final DataFlowRequestSupplier supplier = new DataFlowRequestSupplier();

    private static DataAddress createDataAddress() {
        return DataAddress.Builder.newInstance().type("test-type").build();
    }

    @Test
    void verifyMapping_noInputBody() {
        var contextApi = mock(ContainerRequestContextApi.class);
        var address = createDataAddress();

        var method = HttpMethod.GET;
        var queryParams = "test-query-param";
        var path = "test-path";

        when(contextApi.method()).thenReturn(method);
        when(contextApi.queryParams()).thenReturn(queryParams);
        when(contextApi.path()).thenReturn(path);

        var request = supplier.apply(contextApi, address);

        assertThat(request.getId()).isNotBlank();
        assertThat(request.getDestinationDataAddress().getType()).isEqualTo(AsyncStreamingDataSink.TYPE);
        assertThat(request.getSourceDataAddress().getType()).isEqualTo(address.getType());
        assertThat(request.getProperties()).containsExactlyInAnyOrderEntriesOf(Map.of(
                DataFlowRequestSchema.PATH, path,
                DataFlowRequestSchema.METHOD, method,
                DataFlowRequestSchema.QUERY_PARAMS, queryParams

        ));
    }

    @Test
    void verifyMapping_withInputBody() {
        var contextApi = mock(ContainerRequestContextApi.class);
        var address = createDataAddress();

        var method = HttpMethod.GET;
        var queryParams = "test-query-param";
        var path = "test-path";
        var body = "Test request body";

        when(contextApi.method()).thenReturn(method);
        when(contextApi.queryParams()).thenReturn(queryParams);
        when(contextApi.path()).thenReturn(path);
        when(contextApi.mediaType()).thenReturn(MediaType.TEXT_PLAIN);
        when(contextApi.body()).thenReturn(body);

        var request = supplier.apply(contextApi, address);

        assertThat(request.getId()).isNotBlank();
        assertThat(request.getDestinationDataAddress().getType()).isEqualTo(AsyncStreamingDataSink.TYPE);
        assertThat(request.getSourceDataAddress().getType()).isEqualTo(address.getType());
        assertThat(request.getProperties()).containsExactlyInAnyOrderEntriesOf(Map.of(
                DataFlowRequestSchema.PATH, path,
                DataFlowRequestSchema.METHOD, method,
                DataFlowRequestSchema.QUERY_PARAMS, queryParams,
                DataFlowRequestSchema.BODY, body,
                DataFlowRequestSchema.MEDIA_TYPE, MediaType.TEXT_PLAIN
        ));
    }
}
