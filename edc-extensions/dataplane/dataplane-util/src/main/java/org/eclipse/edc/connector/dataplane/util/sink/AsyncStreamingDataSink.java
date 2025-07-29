/********************************************************************************
 * Copyright (c) 2023,2025 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.dataplane.util.sink;

import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.tractusx.edc.dataplane.http.pipeline.ProxyHttpPart;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult.error;
import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult.success;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;
import static org.eclipse.edc.spi.response.StatusResult.failure;
import static org.eclipse.edc.util.async.AsyncUtils.asyncAllOf;

/**
 * Asynchronously streams data to a response client.
 */
public class AsyncStreamingDataSink implements DataSink {

    public static final String TYPE = "AsyncStreaming";
    private static final String DEFAULT_SUCCESSFUL_RESPONSE_STATUS_CODE = "200";

    private final AsyncResponseContext asyncContext;
    private final ExecutorService executorService;

    public AsyncStreamingDataSink(AsyncResponseContext asyncContext, ExecutorService executorService) {
        this.asyncContext = asyncContext;
        this.executorService = executorService;
    }

    @Override
    public CompletableFuture<StreamResult<Object>> transfer(DataSource source) {
        var streamResult = source.openPartStream();
        if (streamResult.failed()) {
            completedFuture(StreamResult.failure(streamResult.getFailure()));
        }

        try (var partStream = streamResult.getContent()) {
            return partStream
                    .map(part -> supplyAsync(() -> transferPart(part), executorService))
                    .collect(asyncAllOf())
                    .thenApply(this::processResults);
        }
    }

    @NotNull
    private StreamResult<Object> processResults(List<? extends StatusResult<?>> results) {
        if (results.stream().anyMatch(AbstractResult::failed)) {
            return error("Error transferring data");
        }
        return success();
    }

    @NotNull
    private StatusResult<?> transferPart(DataSource.Part part) {
        Map<String, String> proxyHeaders = part instanceof ProxyHttpPart proxyHttpPart ? proxyHttpPart.headers() : Map.of();
        var result = asyncContext.register(new AsyncResponseCallback(outputStream -> {
            try {
                if (proxyPartHasContent(part)) {
                    part.openStream().transferTo(outputStream);
                }
            } catch (IOException e) {
                throw new EdcException(e);
            }
        }, part.mediaType(), extractResponseCode(part), proxyHeaders));

        return result ? StatusResult.success() : failure(FATAL_ERROR, "Could not resume output stream write");
    }

    private static boolean proxyPartHasContent(DataSource.Part part) {
        return !(part instanceof ProxyHttpPart proxyHttpPart) || proxyHttpPart.content() != null;
    }

    private String extractResponseCode(DataSource.Part part) {
        return (part instanceof ProxyHttpPart proxyHttpPart)
                ? proxyHttpPart.statusCode()
                : DEFAULT_SUCCESSFUL_RESPONSE_STATUS_CODE;
    }

    /**
     * Serves as a facade for a response context that writes data asynchronously to a client.
     */
    @FunctionalInterface
    public interface AsyncResponseContext {

        /**
         * Registers a callback when an output stream is available for writing data. The second parameter is the media type.
         *
         * @param callback the callback
         * @return true if the callback was successfully registered
         */
        boolean register(AsyncResponseCallback callback);
    }

    public record AsyncResponseCallback(Consumer<OutputStream> outputStreamConsumer, String mediaType,
                                        String statusCode, Map<String, String> proxyHeaders) {

    }

}
