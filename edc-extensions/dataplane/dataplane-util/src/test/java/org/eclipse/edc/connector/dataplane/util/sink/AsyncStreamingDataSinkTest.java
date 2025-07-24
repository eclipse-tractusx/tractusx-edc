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

package org.eclipse.edc.connector.dataplane.util.sink;

import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.connector.dataplane.util.sink.AsyncStreamingDataSink.AsyncResponseContext;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.tractusx.edc.dataplane.http.pipeline.ProxyHttpPart;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult.success;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AsyncStreamingDataSinkTest {
    private static final byte[] TEST_CONTENT = "test".getBytes();

    private final AsyncResponseContext asyncContext = mock();
    private final ExecutorService executorService = newSingleThreadExecutor();
    private final Monitor monitor = mock();

    private final AsyncStreamingDataSink dataSink = new AsyncStreamingDataSink(asyncContext, executorService);

    @Test
    void verify_streaming() {
        var part = mock(DataSource.Part.class);
        when(part.openStream()).thenReturn(new ByteArrayInputStream(TEST_CONTENT));

        var dataSource = mock(DataSource.class);
        when(dataSource.openPartStream()).thenReturn(success(Stream.of(part)));

        var outputStream = new ByteArrayOutputStream();

        //noinspection unchecked
        when(asyncContext.register(isA(AsyncStreamingDataSink.AsyncResponseCallback.class))).thenAnswer((Answer<Boolean>) invocation -> {
            @SuppressWarnings("rawtypes") var callback = (AsyncStreamingDataSink.AsyncResponseCallback) invocation.getArgument(0);
            callback.outputStreamConsumer().accept(outputStream);
            return true;
        });

        var future = dataSink.transfer(dataSource);

        assertThat(future).succeedsWithin(2, SECONDS).satisfies(result -> {
            assertThat(result).isSucceeded();
            assertThat(outputStream.toByteArray()).isEqualTo(TEST_CONTENT);
        });
    }

    @Test
    void verify_exceptionThrown() throws Exception {
        var part = mock(DataSource.Part.class);
        when(part.openStream()).thenReturn(new ByteArrayInputStream(TEST_CONTENT));

        var dataSource = mock(DataSource.class);
        when(dataSource.openPartStream()).thenReturn(StreamResult.success(Stream.of(part)));

        var outputStream = mock(OutputStream.class);

        var testException = new RuntimeException("Test Exception");

        doThrow(testException).when(outputStream).write(isA(byte[].class), anyInt(), anyInt());

        //noinspection unchecked
        when(asyncContext.register(isA(AsyncStreamingDataSink.AsyncResponseCallback.class))).thenAnswer((Answer<Boolean>) invocation -> {
            @SuppressWarnings("rawtypes") var callback = (AsyncStreamingDataSink.AsyncResponseCallback) invocation.getArgument(0);
            callback.outputStreamConsumer().accept(outputStream);
            return true;
        });

        var future = dataSink.transfer(dataSource);

        assertThat(future).failsWithin(2, SECONDS).withThrowableThat().havingCause().isEqualTo(testException);
    }

    @Test
    void verify_customStatusCode() {
        var part = mock(ProxyHttpPart.class);
        when(part.statusCode()).thenReturn("201");
        when(part.openStream()).thenReturn(new ByteArrayInputStream(TEST_CONTENT));

        var dataSource = mock(DataSource.class);
        when(dataSource.openPartStream()).thenReturn(success(Stream.of(part)));

        when(asyncContext.register(isA(AsyncStreamingDataSink.AsyncResponseCallback.class))).thenAnswer((Answer<Boolean>) invocation -> {
            var callback = (AsyncStreamingDataSink.AsyncResponseCallback) invocation.getArgument(0);
            assertThat(callback.statusCode()).isEqualTo("201");
            return true;
        });

        var future = dataSink.transfer(dataSource);

        assertThat(future).succeedsWithin(2, SECONDS).satisfies(result -> assertThat(result).isSucceeded());
    }

    @Test
    void verify_customMediaType() {
        var part = mock(ProxyHttpPart.class);
        when(part.mediaType()).thenReturn("application/json");
        when(part.openStream()).thenReturn(new ByteArrayInputStream(TEST_CONTENT));

        var dataSource = mock(DataSource.class);
        when(dataSource.openPartStream()).thenReturn(success(Stream.of(part)));

        when(asyncContext.register(isA(AsyncStreamingDataSink.AsyncResponseCallback.class))).thenAnswer((Answer<Boolean>) invocation -> {
            var callback = (AsyncStreamingDataSink.AsyncResponseCallback) invocation.getArgument(0);
            assertThat(callback.mediaType()).isEqualTo("application/json");
            return true;
        });

        var future = dataSink.transfer(dataSource);

        assertThat(future).succeedsWithin(2, SECONDS).satisfies(result -> assertThat(result).isSucceeded());
    }

    @Test
    void verify_expectedHeaders() {
        var headers = Map.of(
                "some-header-test-1", "test1",
                "some-header-test-2", "test2",
                "some-header-test-3", "test3");
        var part = mock(ProxyHttpPart.class);
        when(part.headers()).thenReturn(headers);
        when(part.openStream()).thenReturn(new ByteArrayInputStream(TEST_CONTENT));

        var dataSource = mock(DataSource.class);
        when(dataSource.openPartStream()).thenReturn(success(Stream.of(part)));

        when(asyncContext.register(isA(AsyncStreamingDataSink.AsyncResponseCallback.class))).thenAnswer((Answer<Boolean>) invocation -> {
            var callback = (AsyncStreamingDataSink.AsyncResponseCallback) invocation.getArgument(0);
            assertThat(callback.proxyHeaders()).isEqualTo(headers);
            return true;
        });

        var future = dataSink.transfer(dataSource);

        assertThat(future).succeedsWithin(2, SECONDS).satisfies(result -> assertThat(result).isSucceeded());
    }

    @Test
    void verify_proxyHttpPartWithoutContent() {
        var part = mock(ProxyHttpPart.class);
        when(part.content()).thenReturn(null);

        var dataSource = mock(DataSource.class);
        when(dataSource.openPartStream()).thenReturn(success(Stream.of(part)));

        var outputStream = new ByteArrayOutputStream();

        when(asyncContext.register(isA(AsyncStreamingDataSink.AsyncResponseCallback.class))).thenAnswer((Answer<Boolean>) invocation -> {
            var callback = (AsyncStreamingDataSink.AsyncResponseCallback) invocation.getArgument(0);
            callback.outputStreamConsumer().accept(outputStream);
            return true;
        });

        var future = dataSink.transfer(dataSource);

        assertThat(future).succeedsWithin(2, SECONDS).satisfies(result -> {
            assertThat(result).isSucceeded();
            assertThat(outputStream.size()).isZero();
        });
    }

}
