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

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.message.ProtocolRemoteMessage;
import org.eclipse.tractusx.edc.spi.callback.InProcessCallback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.eclipse.tractusx.edc.callback.TestFunctions.getNegotiationFinalizedEvent;
import static org.eclipse.tractusx.edc.callback.TestFunctions.remoteMessage;
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
        dispatcher.dispatch("any", Object.class, msg).join();

        verify(callback).invoke(msg);
    }

    @Test
    void send_shouldNotInvokeRegisteredCallback_whenItsNotCallbackRemoteMessage() {
        assertThatThrownBy(() -> dispatcher.dispatch("any", Object.class, new TestMessage()).join())
                .hasCauseInstanceOf(EdcException.class);

        verifyNoInteractions(callback);
    }

    private static class TestMessage extends ProtocolRemoteMessage {

        @Override
        public String getProtocol() {
            return "test";
        }

        @Override
        public String getCounterPartyAddress() {
            return "test";
        }

        @Override
        public String getCounterPartyId() {
            return "id";
        }
    }
}
