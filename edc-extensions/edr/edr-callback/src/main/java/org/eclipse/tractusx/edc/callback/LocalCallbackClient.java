/********************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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
 * Assisted-By: Claude Code (claude-opus-5-5)
 ********************************************************************************/

package org.eclipse.tractusx.edc.callback;

import org.eclipse.edc.connector.controlplane.services.spi.callback.CallbackClient;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.tractusx.edc.spi.callback.InProcessCallbackMessage;
import org.eclipse.tractusx.edc.spi.callback.InProcessCallbackRegistry;

import java.net.URI;

import static java.lang.String.format;

/**
 * {@link CallbackClient} that delivers events for callback addresses with the {@code local} scheme (e.g.
 * {@code local://adapter}) to the {@link InProcessCallbackRegistry} and forwards every other callback address to a
 * delegate, which is the upstream HTTP callback client.
 * <p>
 * A failed in-process callback is logged and not propagated, as it was the case with the
 * {@code RemoteMessageDispatcher}-based dispatching before EDC 0.18: propagating it would roll back the transaction
 * of a transactional callback, e.g. the finalization of a contract negotiation.
 */
public class LocalCallbackClient implements CallbackClient {

    public static final String LOCAL = "local";

    private final InProcessCallbackRegistry registry;
    private final CallbackClient delegate;
    private final Monitor monitor;

    public LocalCallbackClient(InProcessCallbackRegistry registry, CallbackClient delegate, Monitor monitor) {
        this.registry = registry;
        this.delegate = delegate;
        this.monitor = monitor;
    }

    @Override
    public <E extends Event> void dispatch(CallbackAddress callbackAddress, EventEnvelope<E> eventEnvelope) {
        if (!isLocal(callbackAddress)) {
            delegate.dispatch(callbackAddress, eventEnvelope);
            return;
        }

        var result = registry.handleMessage(new InProcessCallbackMessage<>(callbackAddress, eventEnvelope));
        if (result.failed()) {
            monitor.warning(format("In-process callback %s for event %s failed: %s", callbackAddress.getUri(),
                    eventEnvelope.getPayload().name(), result.getFailureDetail()));
        }
    }

    private boolean isLocal(CallbackAddress callbackAddress) {
        var uri = callbackAddress.getUri();
        if (uri == null) {
            return false;
        }
        try {
            return LOCAL.equalsIgnoreCase(URI.create(uri).getScheme());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
