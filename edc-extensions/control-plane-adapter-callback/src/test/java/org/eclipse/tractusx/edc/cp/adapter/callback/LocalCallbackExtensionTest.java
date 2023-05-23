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

import org.eclipse.edc.connector.spi.callback.CallbackProtocolResolver;
import org.eclipse.edc.connector.spi.callback.CallbackProtocolResolverRegistry;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.eclipse.tractusx.edc.spi.cp.adapter.callback.InProcessCallback;
import org.eclipse.tractusx.edc.spi.cp.adapter.callback.InProcessCallbackRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.cp.adapter.callback.InProcessCallbackMessageDispatcher.CALLBACK_EVENT_LOCAL;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
public class LocalCallbackExtensionTest {

    LocalCallbackExtension extension;

    RemoteMessageDispatcherRegistry dispatcherRegistry = mock(RemoteMessageDispatcherRegistry.class);

    CallbackProtocolResolverRegistry resolverRegistry = mock(CallbackProtocolResolverRegistry.class);

    InProcessCallbackRegistry inProcessCallbackRegistry = mock(InProcessCallbackRegistry.class);

    @BeforeEach
    void setUp(ObjectFactory factory, ServiceExtensionContext context) {

        context.registerService(RemoteMessageDispatcherRegistry.class, dispatcherRegistry);
        context.registerService(CallbackProtocolResolverRegistry.class, resolverRegistry);
        context.registerService(InProcessCallbackRegistry.class, inProcessCallbackRegistry);
        extension = factory.constructInstance(LocalCallbackExtension.class);
    }

    @Test
    void shouldInitializeTheExtension(ServiceExtensionContext context) {
        extension.initialize(context);

        var captor = ArgumentCaptor.forClass(CallbackProtocolResolver.class);
        verify(resolverRegistry).registerResolver(captor.capture());

        var resolver = captor.getValue();
        assertThat(resolver.resolve("local")).isEqualTo(CALLBACK_EVENT_LOCAL);
        assertThat(resolver.resolve("test")).isNull();


        var service = extension.adapterTransferProcessService();
        assertThat(service).isInstanceOf(AdapterTransferProcessServiceImpl.class);

        var callbackArgumentCaptor = ArgumentCaptor.forClass(InProcessCallback.class);
        verify(inProcessCallbackRegistry, times(2)).registerHandler(callbackArgumentCaptor.capture());

        assertThat(callbackArgumentCaptor.getAllValues())
                .flatExtracting(Object::getClass)
                .containsExactly(ContractNegotiationCallback.class, TransferProcessLocalCallback.class);
    }
}
