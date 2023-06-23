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

package org.eclipse.tractusx.edc.cp.adapter.callback;

import org.eclipse.edc.connector.spi.callback.CallbackEventRemoteMessage;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.message.RemoteMessageDispatcher;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;
import org.eclipse.tractusx.edc.spi.cp.adapter.callback.InProcessCallbackRegistry;

import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;

public class InProcessCallbackMessageDispatcher implements RemoteMessageDispatcher {

    public static final String CALLBACK_EVENT_LOCAL = "callback-event-local";

    private final InProcessCallbackRegistry registry;

    public InProcessCallbackMessageDispatcher(InProcessCallbackRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String protocol() {
        return CALLBACK_EVENT_LOCAL;
    }

    @Override
    public <T, M extends RemoteMessage> CompletableFuture<StatusResult<T>> dispatch(Class<T> responseType, M message) {
        if (message instanceof CallbackEventRemoteMessage) {
            var result = registry.handleMessage((CallbackEventRemoteMessage<? extends Event>) message);
            if (result.succeeded()) {
                return CompletableFuture.completedFuture(StatusResult.success(null));
            } else {
                return CompletableFuture.completedFuture(StatusResult.failure(ResponseStatus.FATAL_ERROR, result.getFailureDetail()));
            }
        }
        return CompletableFuture.failedFuture(new EdcException(format("Message of type %s not supported", message.getClass().getSimpleName())));
    }
}
