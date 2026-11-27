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

package org.eclipse.tractusx.edc.spi.callback;

import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;

import java.util.Objects;

/**
 * An event that is delivered to an {@link InProcessCallback} because a {@link CallbackAddress} with the
 * {@code local} scheme subscribed to it.
 * <p>
 * Replaces the upstream {@code CallbackEventRemoteMessage}, which was removed in EDC 0.18.
 *
 * @param <T> the event type
 */
public final class InProcessCallbackMessage<T extends Event> {

    private final CallbackAddress callbackAddress;
    private final EventEnvelope<T> eventEnvelope;

    public InProcessCallbackMessage(CallbackAddress callbackAddress, EventEnvelope<T> eventEnvelope) {
        this.callbackAddress = Objects.requireNonNull(callbackAddress, "callbackAddress");
        this.eventEnvelope = Objects.requireNonNull(eventEnvelope, "eventEnvelope");
    }

    public CallbackAddress getCallbackAddress() {
        return callbackAddress;
    }

    public EventEnvelope<T> getEventEnvelope() {
        return eventEnvelope;
    }
}
