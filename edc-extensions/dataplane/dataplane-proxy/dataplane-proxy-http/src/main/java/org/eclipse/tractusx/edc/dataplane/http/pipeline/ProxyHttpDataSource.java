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


import okhttp3.MediaType;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.edc.connector.dataplane.http.params.HttpRequestFactory;
import org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParams;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress.OCTET_STREAM;
import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult.success;

public class ProxyHttpDataSource implements DataSource {

    private String name;
    private HttpRequestParams params;
    private String requestId;
    private Monitor monitor;
    private EdcHttpClient httpClient;
    private HttpRequestFactory requestFactory;
    private final AtomicReference<ResponseBodyStream> responseBodyStream = new AtomicReference<>();

    private ProxyHttpDataSource() {
    }

    @Override
    public StreamResult<Stream<Part>> openPartStream() {
        var request = requestFactory.toRequest(params);
        monitor.debug(() -> "Executing HTTP request: " + request.url().url());
        try {
            // NB: Do not close the response as the body input stream needs to be read after this method returns. The response closes the body stream.
            var response = httpClient.execute(request);
            var statusCode = (String.valueOf(response.code()));

            return handleResponse(response, statusCode);
        } catch (IOException e) {
            monitor.severe("Exception during executing HTTP request: " + request.url().url(), e);
            throw new EdcException(e);
        }
    }

    private StreamResult<Stream<Part>> handleResponse(Response response, String statusCode) {
        var body = response.body();
        InputStream stream = null;
        var mediaType = OCTET_STREAM;
        if (body != null) {
            stream = body.byteStream();
            responseBodyStream.set(new ResponseBodyStream(body, stream));
            mediaType = Optional.ofNullable(body.contentType()).map(MediaType::toString).orElse(OCTET_STREAM);
        }

        Stream<Part> content = Stream.of(new ProxyHttpPart(name, stream, mediaType, statusCode, extractHeaders(response)));
        return success(content);
    }

    private Map<String, String> extractHeaders(Response response) {
        var proxyHeaders = new HashMap<String, String>();
        response.headers().forEach(header -> proxyHeaders.put(header.getFirst(), header.getSecond()));
        return proxyHeaders;
    }

    @Override
    public void close() {
        var bodyStream = responseBodyStream.get();
        if (bodyStream != null) {
            bodyStream.responseBody().close();
            try {
                bodyStream.stream().close();
            } catch (IOException e) {
                // do nothing
            }
        }
    }

    private record ResponseBodyStream(ResponseBody responseBody, InputStream stream) {

    }

    public static class Builder {
        private final ProxyHttpDataSource dataSource;

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
            dataSource = new ProxyHttpDataSource();
        }

        public Builder params(HttpRequestParams params) {
            dataSource.params = params;
            return this;
        }

        public Builder name(String name) {
            dataSource.name = name;
            return this;
        }

        public Builder requestId(String requestId) {
            dataSource.requestId = requestId;
            return this;
        }

        public Builder httpClient(EdcHttpClient httpClient) {
            dataSource.httpClient = httpClient;
            return this;
        }

        public Builder monitor(Monitor monitor) {
            dataSource.monitor = monitor;
            return this;
        }

        public Builder requestFactory(HttpRequestFactory requestFactory) {
            dataSource.requestFactory = requestFactory;
            return this;
        }

        public ProxyHttpDataSource build() {
            Objects.requireNonNull(dataSource.requestId, "requestId");
            Objects.requireNonNull(dataSource.httpClient, "httpClient");
            Objects.requireNonNull(dataSource.monitor, "monitor");
            Objects.requireNonNull(dataSource.requestFactory, "requestFactory");
            return dataSource;
        }
    }

}
