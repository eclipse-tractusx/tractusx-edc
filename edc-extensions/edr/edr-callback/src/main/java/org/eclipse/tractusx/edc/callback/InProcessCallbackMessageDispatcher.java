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

package org.eclipse.tractusx.edc.callback;

import org.eclipse.edc.connector.controlplane.services.spi.callback.CallbackEventRemoteMessage;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.message.RemoteMessageDispatcher;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;
import org.eclipse.tractusx.edc.spi.callback.InProcessCallbackRegistry;

import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;

public class InProcessCallbackMessageDispatcher implements RemoteMessageDispatcher<RemoteMessage> {

    public static final String CALLBACK_EVENT_LOCAL = "callback-event-local";

    private final InProcessCallbackRegistry registry;

    public InProcessCallbackMessageDispatcher(InProcessCallbackRegistry registry) {
        this.registry = registry;
    }

    @Override
    public <T, M extends RemoteMessage> CompletableFuture<StatusResult<T>> dispatch(String participantContextId, Class<T> responseType, M message) {
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
