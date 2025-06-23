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

package org.eclipse.edc.tractusx.non.finite.provider.push.core.pipeline;

import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSinkFactory;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.edc.connector.dataplane.spi.pipeline.InputStreamDataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.tractusx.non.finite.provider.push.spi.FinitenessEvaluator;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult.failure;
import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult.success;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.util.async.AsyncUtils.asyncAllOf;
import static org.mockito.Mockito.mock;

class NonFiniteCapablePipelineServiceIntegrationTest {

    private final Monitor monitor = mock();
    private final FinitenessEvaluator finitenessEvaluator = mock();
    private final NonFiniteCapablePipelineService pipelineService = new NonFiniteCapablePipelineService(monitor, finitenessEvaluator);

    private static class InputStreamDataSourceFactory implements DataSourceFactory {

        private static final List<String> DATA = List.of("foo", "bar", "baz");

        private int count = 0;

        @Override
        public String supportedType() {
            return "any";
        }

        @Override
        public DataSource createSource(DataFlowStartMessage request) {
            var bytes = DATA.get(count).getBytes();
            count++;
            return new InputStreamDataSource("test", new ByteArrayInputStream(bytes));
        }

        @Override
        public @NotNull Result<Void> validateRequest(DataFlowStartMessage request) {
            return Result.success();
        }

    }

    private static class MemorySink implements DataSink {

        private final ByteArrayOutputStream bos;

        MemorySink() {
            bos = new ByteArrayOutputStream();
        }

        @Override
        public CompletableFuture<StreamResult<Object>> transfer(DataSource source) {
            var streamResult = source.openPartStream();
            if (streamResult.failed()) {
                return completedFuture(failure(streamResult.getFailure()));
            }
            var partStream = streamResult.getContent();
            return partStream
                    .map(part -> supplyAsync(() -> transferTo(part, bos)))
                    .collect(asyncAllOf())
                    .thenApply(longs -> success(bos.toByteArray()));
        }

        private long transferTo(DataSource.Part part, OutputStream stream) {
            try {
                return part.openStream().transferTo(stream);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    private static class MemorySinkFactory implements DataSinkFactory {

        @Override
        public String supportedType() {
            return "any";
        }

        @Override
        public DataSink createSink(DataFlowStartMessage request) {
            return new MemorySink();
        }

        @Override
        public @NotNull Result<Void> validateRequest(DataFlowStartMessage request) {
            return Result.success();
        }
    }

    private DataFlowStartMessage createRequest() {
        return DataFlowStartMessage.Builder.newInstance()
                .id("1")
                .processId("1")
                .sourceDataAddress(DataAddress.Builder.newInstance().type("any").build())
                .destinationDataAddress(DataAddress.Builder.newInstance().type("any").build())
                .build();
    }

    @Test
    void transferData() {
        pipelineService.registerFactory(new InputStreamDataSourceFactory());
        pipelineService.registerFactory(new MemorySinkFactory());

        var future = pipelineService.transfer(createRequest());

        assertThat(future).succeedsWithin(5, SECONDS)
                .satisfies(result -> assertThat(result).isSucceeded()
                        .isEqualTo(InputStreamDataSourceFactory.DATA.get(0).getBytes()));
    }

    @Test
    void transferData_withCustomSink() {
        pipelineService.registerFactory(new InputStreamDataSourceFactory());

        var future = pipelineService.transfer(createRequest(), new MemorySink());

        assertThat(future).succeedsWithin(5, SECONDS).satisfies(result -> {
            assertThat(result).isSucceeded().isInstanceOf(byte[].class);
            var bytes = (byte[]) result.getContent();
            assertThat(bytes).isEqualTo(InputStreamDataSourceFactory.DATA.get(0).getBytes());
        });
    }

    @Test
    void transferNonFiniteData() {
        pipelineService.registerFactory(new InputStreamDataSourceFactory());
        pipelineService.registerFactory(new MemorySinkFactory());

        for (var data : InputStreamDataSourceFactory.DATA) {
            var future = pipelineService.transfer(createRequest());

            assertThat(future).succeedsWithin(5, SECONDS)
                    .satisfies(StreamResult::succeeded)
                    .satisfies(result -> result.getContent().equals(data));
        }
    }

}
