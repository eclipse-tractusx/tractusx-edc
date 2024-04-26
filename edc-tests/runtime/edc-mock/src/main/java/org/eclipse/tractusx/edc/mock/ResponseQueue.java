/*
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft
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
 */

package org.eclipse.tractusx.edc.mock;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;

import java.util.Arrays;
import java.util.List;
import java.util.Queue;

public class ResponseQueue {
    private final Queue<RecordedRequest<?, ?>> recordedRequests; // todo guard access with locks
    private final Monitor monitor;

    public ResponseQueue(Queue<RecordedRequest<?, ?>> recordedRequests, Monitor monitor) {
        this.recordedRequests = recordedRequests;
        this.monitor = monitor;
    }

    public <T> ServiceResult<T> getNext(Class<T> outputClass, String errorMessageTemplate) {
        try {
            return getNext(outputClass);
        } catch (ClassCastException ex) {
            var message = errorMessageTemplate.formatted(ex.getMessage());
            monitor.severe(message); // no need for the entire stack trace
            return ServiceResult.badRequest(message);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> ServiceResult<List<T>> getNextAsList(Class<T> arrayElementClass, String errorMessageTemplate) {
        if (arrayElementClass.isArray()) {
            return ServiceResult.badRequest("First parameter must be type of list elements. For example, pass Object.class if a List<Object> is expected, but '%s' was passed".formatted(arrayElementClass.getName()));
        }
        var r = Result.ofThrowable(() -> {
            T[] serviceResult = (T[]) getNext(arrayElementClass.arrayType(), errorMessageTemplate).orElseThrow(f -> new InvalidRequestException(f.getFailureDetail()));
            return Arrays.asList(serviceResult);
        });
        if (r.succeeded()) {
            return ServiceResult.success(r.getContent());
        }
        monitor.severe(errorMessageTemplate.formatted(r.getFailureDetail()));
        return ServiceResult.badRequest(r.getFailureDetail());
    }

    public void clear() {
        recordedRequests.clear();
    }

    public void append(RecordedRequest<?, ?> recordedRequest) {
        recordedRequests.offer(recordedRequest);
    }

    public List<RecordedRequest<?, ?>> toList() {
        return recordedRequests.stream().toList(); //immutable
    }

    @SuppressWarnings("unchecked")
    private <T> ServiceResult<T> getNext(Class<T> outputType) {
        monitor.debug("Get next recorded request, expect output of type %s".formatted(outputType));
        var r = recordedRequests.poll();


        if (r != null) {
            monitor.debug("Recorded request fetched, %d remaining.".formatted(recordedRequests.size()));
            var recipeOutputType = r.getOutput().getClass();
            if (!recipeOutputType.isAssignableFrom(outputType)) {
                return ServiceResult.badRequest("Type mismatch: service invocation requires '%s', but Recipe specifies '%s'".formatted(outputType, recipeOutputType));
            }
            var output = (T) r.getOutput();
            return ServiceResult.success(output);
        }
        var message = "Failure: no recorded request left in queue.";
        monitor.debug(message);
        return ServiceResult.badRequest(message);
    }

    //todo: add method getNextWithMatch that accepts an input and a match type
}
