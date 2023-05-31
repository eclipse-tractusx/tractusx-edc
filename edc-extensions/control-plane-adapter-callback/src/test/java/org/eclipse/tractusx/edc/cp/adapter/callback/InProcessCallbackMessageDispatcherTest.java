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

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;
import org.eclipse.tractusx.edc.spi.cp.adapter.callback.InProcessCallback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.eclipse.tractusx.edc.cp.adapter.callback.TestFunctions.getNegotiationFinalizedEvent;
import static org.eclipse.tractusx.edc.cp.adapter.callback.TestFunctions.remoteMessage;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class InProcessCallbackMessageDispatcherTest {

    InProcessCallback callback = mock(InProcessCallback.class);

    InProcessCallbackMessageDispatcher dispatcher;

    @BeforeEach
    void setup() {
        var registry = new InProcessCallbackRegistryImpl();
        registry.registerHandler(callback);
        dispatcher = new InProcessCallbackMessageDispatcher(registry);
    }

    @Test
    void send_shouldInvokeRegisteredCallback() {

        var msg = remoteMessage(getNegotiationFinalizedEvent());
        when(callback.invoke(any())).thenReturn(Result.success());
        dispatcher.send(Object.class, msg).join();


        verify(callback).invoke(msg);
    }

    @Test
    void send_shouldNotInvokeRegisteredCallback_whenItsNotCallbackRemoteMessage() {

        assertThatThrownBy(() -> dispatcher.send(Object.class, new TestMessage()).join())
                .hasCauseInstanceOf(EdcException.class);


        verifyNoInteractions(callback);
    }

    private static class TestMessage implements RemoteMessage {

        @Override
        public String getProtocol() {
            return "test";
        }

        @Override
        public String getCounterPartyAddress() {
            return "test";
        }
    }
}
