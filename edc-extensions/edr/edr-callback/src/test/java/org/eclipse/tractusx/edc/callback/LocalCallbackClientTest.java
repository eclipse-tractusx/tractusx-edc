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
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.tractusx.edc.spi.callback.InProcessCallback;
import org.eclipse.tractusx.edc.spi.callback.InProcessCallbackMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.tractusx.edc.callback.TestFunctions.getNegotiationFinalizedEvent;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LocalCallbackClientTest {

    private final InProcessCallback callback = mock();
    private final CallbackClient delegate = mock();
    private final Monitor monitor = mock();
    private LocalCallbackClient client;

    @BeforeEach
    void setup() {
        var registry = new InProcessCallbackRegistryImpl();
        registry.registerHandler(callback);
        client = new LocalCallbackClient(registry, delegate, monitor);
    }

    @ParameterizedTest
    @ValueSource(strings = { "local://adapter", "LOCAL://adapter" })
    void dispatch_shouldInvokeInProcessCallback_whenSchemeIsLocal(String uri) {
        when(callback.invoke(any())).thenReturn(Result.success());
        var address = callbackAddress(uri);
        var envelope = envelope();

        client.dispatch(address, envelope);

        ArgumentCaptor<InProcessCallbackMessage<?>> captor = ArgumentCaptor.captor();
        verify(callback).invoke(captor.capture());
        assertThat(captor.getValue().getCallbackAddress()).isSameAs(address);
        assertThat(captor.getValue().getEventEnvelope()).isSameAs(envelope);
        verifyNoInteractions(delegate);
    }

    @Test
    void dispatch_shouldLogAndNotThrow_whenInProcessCallbackFails() {
        when(callback.invoke(any())).thenReturn(Result.failure("error"));

        assertThatNoException().isThrownBy(() -> client.dispatch(callbackAddress("local://adapter"), envelope()));

        verify(monitor).warning(anyString());
        verifyNoInteractions(delegate);
    }

    @Test
    void dispatch_shouldDelegate_whenSchemeIsNotLocal() {
        var address = callbackAddress("https://example.com/callback");
        var envelope = envelope();

        client.dispatch(address, envelope);

        verify(delegate).dispatch(address, envelope);
        verifyNoInteractions(callback);
    }

    @Test
    void dispatch_shouldPropagateDelegateFailure() {
        var address = callbackAddress("https://example.com/callback");
        var envelope = envelope();
        doThrow(new EdcException("failure")).when(delegate).dispatch(address, envelope);

        assertThatThrownBy(() -> client.dispatch(address, envelope)).isInstanceOf(EdcException.class);
    }

    private CallbackAddress callbackAddress(String uri) {
        return CallbackAddress.Builder.newInstance().uri(uri).events(Set.of("contract.negotiation")).transactional(true).build();
    }

    private EventEnvelope<?> envelope() {
        return EventEnvelope.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .at(System.currentTimeMillis())
                .payload(getNegotiationFinalizedEvent())
                .build();
    }
}
