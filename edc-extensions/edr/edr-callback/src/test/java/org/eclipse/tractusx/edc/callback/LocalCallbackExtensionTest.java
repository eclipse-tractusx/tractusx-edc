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

import org.eclipse.edc.connector.controlplane.services.spi.callback.CallbackProtocolResolver;
import org.eclipse.edc.connector.controlplane.services.spi.callback.CallbackProtocolResolverRegistry;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.tractusx.edc.spi.callback.InProcessCallback;
import org.eclipse.tractusx.edc.spi.callback.InProcessCallbackRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.callback.InProcessCallbackMessageDispatcher.CALLBACK_EVENT_LOCAL;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
public class LocalCallbackExtensionTest {

    RemoteMessageDispatcherRegistry dispatcherRegistry = mock(RemoteMessageDispatcherRegistry.class);

    CallbackProtocolResolverRegistry resolverRegistry = mock(CallbackProtocolResolverRegistry.class);

    InProcessCallbackRegistry inProcessCallbackRegistry = mock(InProcessCallbackRegistry.class);

    @BeforeEach
    void setUp(ServiceExtensionContext context) {

        context.registerService(RemoteMessageDispatcherRegistry.class, dispatcherRegistry);
        context.registerService(CallbackProtocolResolverRegistry.class, resolverRegistry);
        context.registerService(InProcessCallbackRegistry.class, inProcessCallbackRegistry);
    }

    @Test
    void shouldInitializeTheExtension(ServiceExtensionContext context, LocalCallbackExtension extension) {
        extension.initialize(context);

        var captor = ArgumentCaptor.forClass(CallbackProtocolResolver.class);
        verify(resolverRegistry).registerResolver(captor.capture());

        var resolver = captor.getValue();
        assertThat(resolver.resolve("local")).isEqualTo(CALLBACK_EVENT_LOCAL);
        assertThat(resolver.resolve("test")).isNull();

        var callbackArgumentCaptor = ArgumentCaptor.forClass(InProcessCallback.class);
        verify(inProcessCallbackRegistry, times(1)).registerHandler(callbackArgumentCaptor.capture());

        assertThat(callbackArgumentCaptor.getAllValues())
                .flatExtracting(Object::getClass)
                .containsExactly(ContractNegotiationCallback.class);
    }
}
