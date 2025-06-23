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

import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSinkFactory;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamFailure;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.tractusx.non.finite.provider.push.spi.FinitenessEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamFailure.Reason.GENERAL_ERROR;
import static org.eclipse.edc.connector.dataplane.spi.pipeline.StreamFailure.Reason.NOT_FOUND;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NonFiniteCapablePipelineServiceTest {

    private static final String PROCESS_ID = "1";
    private static final String SUPPORTED_SOURCE_TYPE = "source";
    private static final String SUPPORTED_DESTINATION_TYPE = "destination";

    private final DataSourceFactory sourceFactory = mock();
    private final DataSinkFactory sinkFactory = mock();
    private final DataSource source = mock();
    private final DataSink sink = mock();

    private final Monitor monitor = mock();
    private final FinitenessEvaluator finitenessEvaluator = mock();
    private final NonFiniteCapablePipelineService service = new NonFiniteCapablePipelineService(monitor, finitenessEvaluator);

    @BeforeEach
    void setUp() {
        service.registerFactory(sourceFactory);
        service.registerFactory(sinkFactory);
    }

    private DataFlow dataFlow(String sourceType, String destinationType) {
        return DataFlow.Builder.newInstance()
                .id(PROCESS_ID)
                .source(DataAddress.Builder.newInstance().type(sourceType).build())
                .destination(DataAddress.Builder.newInstance().type(destinationType).build())
                .build();
    }

    private static class CanHandleArguments implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Stream.of(
                    arguments(SUPPORTED_SOURCE_TYPE, SUPPORTED_DESTINATION_TYPE, true),
                    arguments("unsupported_source", SUPPORTED_DESTINATION_TYPE, false),
                    arguments(SUPPORTED_SOURCE_TYPE, "unsupported_destination", false),
                    arguments("unsupported_source", "unsupported_destination", false));
        }
    }

    @ParameterizedTest
    @ArgumentsSource(CanHandleArguments.class)
    void canHandle_shouldReturnTrue_whenSourceAndDestinationCanBeHandled(
            String source,
            String destination,
            boolean expected) {
        when(sourceFactory.supportedType()).thenReturn(SUPPORTED_SOURCE_TYPE);
        when(sinkFactory.supportedType()).thenReturn(SUPPORTED_DESTINATION_TYPE);

        boolean result = service.canHandle(dataFlow(source, destination).toRequest());

        assertThat(result).isEqualTo(expected);
    }

    @Nested
    class Transfer {

        @Test
        void transfer_shouldFail_withUnknownSink() {
            var flowRequest = dataFlow(SUPPORTED_SOURCE_TYPE, "custom-destination").toRequest();
            var expectedErrorMessage = format(
                    "GENERAL_ERROR: Unknown data sink type custom-destination for flow id: %s.",
                    PROCESS_ID);

            var future = service.transfer(flowRequest);

            assertThat(future).succeedsWithin(5, SECONDS)
                    .satisfies(res -> assertThat(res).isFailed())
                    .satisfies(res -> assertThat(res.getFailure().getMessages()).hasSize(1))
                    .satisfies(res -> assertThat(res.getFailureDetail()).isEqualTo(expectedErrorMessage));
            verify(sinkFactory).supportedType();
        }

        @Test
        void transfer_shouldFail_withUnknownSource() {
            var flowRequest = dataFlow("wrong-source", SUPPORTED_DESTINATION_TYPE).toRequest();
            var expectedErrorMessage = format(
                    "GENERAL_ERROR: Unknown data source type wrong-source for flow id: %s.",
                    PROCESS_ID);

            when(sinkFactory.supportedType()).thenReturn(SUPPORTED_DESTINATION_TYPE);
            when(sinkFactory.createSink(any())).thenReturn(sink);

            var future = service.transfer(flowRequest);

            assertThat(future).succeedsWithin(5, SECONDS)
                    .satisfies(res -> assertThat(res).isFailed())
                    .satisfies(res -> assertThat(res.getFailure().getMessages()).hasSize(1))
                    .satisfies(res -> assertThat(res.getFailureDetail()).isEqualTo(expectedErrorMessage));
            verify(sourceFactory).supportedType();
        }

        @Test
        void transfer_shouldSucceed_withDefaultSink() throws Exception {
            var flowRequest = dataFlow(SUPPORTED_SOURCE_TYPE, SUPPORTED_DESTINATION_TYPE).toRequest();

            when(sourceFactory.supportedType()).thenReturn(SUPPORTED_SOURCE_TYPE);
            when(sourceFactory.createSource(any())).thenReturn(source);
            when(sinkFactory.supportedType()).thenReturn(SUPPORTED_DESTINATION_TYPE);
            when(sinkFactory.createSink(any())).thenReturn(sink);
            when(sink.transfer(any())).thenReturn(completedFuture(StreamResult.success()));
            when(finitenessEvaluator.isNonFinite(any(DataFlowStartMessage.class))).thenReturn(false);

            var future = service.transfer(flowRequest);

            assertThat(future).succeedsWithin(5, SECONDS)
                    .satisfies(res -> assertThat(res).isSucceeded());
            verify(sinkFactory).createSink(flowRequest);
            verify(sourceFactory).createSource(flowRequest);
            verify(sink).transfer(source);
            verify(source).close();
        }

        @Test
        void transfer_shouldSucceed_withCustomSink() throws Exception {
            var flowRequest = dataFlow(SUPPORTED_SOURCE_TYPE, "custom-destination").toRequest();

            when(sourceFactory.supportedType()).thenReturn(SUPPORTED_SOURCE_TYPE);
            when(sourceFactory.createSource(any())).thenReturn(source);
            when(sink.transfer(any())).thenReturn(completedFuture(StreamResult.success()));
            when(finitenessEvaluator.isNonFinite(any(DataFlowStartMessage.class))).thenReturn(false);

            var customSink = new DataSink() {
                @Override
                public CompletableFuture<StreamResult<Object>> transfer(DataSource source) {
                    return CompletableFuture.completedFuture(StreamResult.success("test-response"));
                }
            };
            var future = service.transfer(flowRequest, customSink);

            assertThat(future).succeedsWithin(5, SECONDS)
                    .satisfies(res -> assertThat(res).isSucceeded()
                            .satisfies(obj -> assertThat(obj).isEqualTo("test-response")));
            verifyNoInteractions(sinkFactory);
            verify(sourceFactory).createSource(flowRequest);
            verify(source).close();
        }

        @Test
        void transfer_shouldReturnIncompleteFuture_whenNonFinite() throws Exception {
            var flowRequest = dataFlow(SUPPORTED_SOURCE_TYPE, SUPPORTED_DESTINATION_TYPE).toRequest();

            when(sourceFactory.supportedType()).thenReturn(SUPPORTED_SOURCE_TYPE);
            when(sourceFactory.createSource(any())).thenReturn(source);
            when(sinkFactory.supportedType()).thenReturn(SUPPORTED_DESTINATION_TYPE);
            when(sinkFactory.createSink(any())).thenReturn(sink);
            when(sink.transfer(any())).thenReturn(completedFuture(StreamResult.success()));
            when(finitenessEvaluator.isNonFinite(any(DataFlowStartMessage.class))).thenReturn(true);

            var future = service.transfer(flowRequest);

            assertThat(future).isNotCompleted();
        }

        @Test
        void transfer_shouldFail_whenNonFiniteAndTransferFailed() throws Exception {
            var flowRequest = dataFlow(SUPPORTED_SOURCE_TYPE, SUPPORTED_DESTINATION_TYPE).toRequest();

            when(sourceFactory.supportedType()).thenReturn(SUPPORTED_SOURCE_TYPE);
            when(sourceFactory.createSource(any())).thenReturn(source);
            when(sinkFactory.supportedType()).thenReturn(SUPPORTED_DESTINATION_TYPE);
            when(sinkFactory.createSink(any())).thenReturn(sink);
            when(sink.transfer(any())).thenReturn(
                    completedFuture(StreamResult.failure(new StreamFailure(List.of("error"), GENERAL_ERROR))));
            when(finitenessEvaluator.isNonFinite(any(DataFlowStartMessage.class))).thenReturn(true);

            var future = service.transfer(flowRequest);

            assertThat(future).succeedsWithin(5, SECONDS)
                    .satisfies(res -> assertThat(res).isFailed())
                    .satisfies(res -> res.getFailureDetail().equals("error"));
        }

        @Nested
        class Terminate {

            @Test
            void terminate_shouldCloseDataSource() throws Exception {
                var dataFlow = dataFlow(SUPPORTED_SOURCE_TYPE, SUPPORTED_DESTINATION_TYPE);

                when(sourceFactory.supportedType()).thenReturn(SUPPORTED_SOURCE_TYPE);
                when(sourceFactory.createSource(any())).thenReturn(source);
                when(sinkFactory.supportedType()).thenReturn(SUPPORTED_DESTINATION_TYPE);
                when(sinkFactory.createSink(any())).thenReturn(sink);
                when(sink.transfer(any())).thenReturn(new CompletableFuture<>());
                when(finitenessEvaluator.isNonFinite(any(DataFlowStartMessage.class))).thenReturn(false);

                service.transfer(dataFlow.toRequest());
                var result = service.terminate(dataFlow);

                assertThat(result).isSucceeded();
                verify(source).close();
            }

            @Test
            void terminate_shouldFail_whenSourceClosureFails() throws Exception {
                var dataFlow = dataFlow(SUPPORTED_SOURCE_TYPE, SUPPORTED_DESTINATION_TYPE);

                when(sourceFactory.supportedType()).thenReturn(SUPPORTED_SOURCE_TYPE);
                when(sourceFactory.createSource(any())).thenReturn(source);
                when(sinkFactory.supportedType()).thenReturn(SUPPORTED_DESTINATION_TYPE);
                when(sinkFactory.createSink(any())).thenReturn(sink);
                when(sink.transfer(any())).thenReturn(new CompletableFuture<>());
                when(finitenessEvaluator.isNonFinite(any(DataFlowStartMessage.class))).thenReturn(false);
                doThrow(IOException.class).when(source).close();

                service.transfer(dataFlow.toRequest());
                var result = service.terminate(dataFlow);

                assertThat(result).isFailed().extracting(StreamFailure::getReason).isEqualTo(GENERAL_ERROR);
                verify(source).close();
            }

            @Test
            void terminate_shouldFail_whenTransferDoesNotExist() {
                var dataFlow = dataFlow(SUPPORTED_SOURCE_TYPE, SUPPORTED_DESTINATION_TYPE);

                var result = service.terminate(dataFlow);

                assertThat(result).isFailed().extracting(StreamFailure::getReason).isEqualTo(NOT_FOUND);
                verifyNoInteractions(source);
            }
        }

        @Nested
        class CloseAll {

            @Test
            void closeAll_shouldCloseAllOngoingDataFlows() throws Exception {
                when(sourceFactory.supportedType()).thenReturn(SUPPORTED_SOURCE_TYPE);
                when(sourceFactory.createSource(any())).thenReturn(source);
                when(sinkFactory.supportedType()).thenReturn(SUPPORTED_DESTINATION_TYPE);
                when(sinkFactory.createSink(any())).thenReturn(sink);
                when(sink.transfer(any())).thenReturn(new CompletableFuture<>());

                service.transfer(dataFlow(SUPPORTED_SOURCE_TYPE, SUPPORTED_DESTINATION_TYPE).toRequest());
                service.closeAll();

                verify(source).close();
            }
        }

        @Nested
        class SupportedTypes {

            @Test
            void supportedSourceTypes_shouldReturnSourceTypesFromFactories() {
                when(sourceFactory.supportedType()).thenReturn(SUPPORTED_SOURCE_TYPE);

                var result = service.supportedSourceTypes();

                assertThat(result).containsOnly(SUPPORTED_SOURCE_TYPE);
                verifyNoInteractions(sinkFactory);
            }

            @Test
            void supportedSinkTypes_shouldReturnSinkTypesFromFactories() {
                when(sinkFactory.supportedType()).thenReturn(SUPPORTED_DESTINATION_TYPE);

                var result = service.supportedSinkTypes();

                assertThat(result).containsOnly(SUPPORTED_DESTINATION_TYPE);
                verifyNoInteractions(sourceFactory);
            }

        }

    }

}
