/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.iam.ssi.miw.api;

import dev.failsafe.Fallback;
import dev.failsafe.event.ExecutionAttemptedEvent;
import dev.failsafe.function.CheckedFunction;
import okhttp3.Response;
import org.eclipse.edc.spi.http.EdcHttpClientException;
import org.eclipse.edc.spi.http.FallbackFactory;

import java.util.Arrays;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class MiwFallbackFactories {

    static FallbackFactory retryWhenStatusIsNot(int status) {
        return retryWhenStatusIsNotIn(status);
    }

    /**
     * Verifies that the response has a specific statuses, otherwise it should be retried
     *
     * @return the {@link FallbackFactory}
     */
    static FallbackFactory retryWhenStatusIsNotIn(int... status) {
        var codes = Arrays.stream(status).boxed().collect(Collectors.toSet());
        return request -> {
            CheckedFunction<ExecutionAttemptedEvent<? extends Response>, Exception> exceptionSupplier = event -> {
                var response = event.getLastResult();
                if (response == null) {
                    return new EdcHttpClientException(event.getLastException().getMessage());
                } else {
                    return new EdcHttpClientException(format("Server response to [%s, %s] was not one of %s but was %s", request.method(), request.url(), Arrays.toString(status), response.code()));
                }
            };
            return Fallback.builderOfException(exceptionSupplier)
                    .handleResultIf(r -> !codes.contains(r.code()))
                    .build();
        };
    }
}
