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

package org.eclipse.tractusx.edc.callback;

import org.eclipse.edc.connector.spi.callback.CallbackEventRemoteMessage;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.tractusx.edc.spi.callback.InProcessCallback;
import org.eclipse.tractusx.edc.spi.callback.InProcessCallbackRegistry;

import java.util.ArrayList;
import java.util.List;

public class InProcessCallbackRegistryImpl implements InProcessCallbackRegistry {

    private final List<InProcessCallback> handlers = new ArrayList<>();

    @Override
    public void registerHandler(InProcessCallback callback) {
        handlers.add(callback);
    }

    @Override
    public <T extends Event> Result<Void> handleMessage(CallbackEventRemoteMessage<T> message) {
        return handlers.stream()
                .map(handler -> handler.invoke(message))
                .filter(Result::failed)
                .findFirst()
                .orElseGet(Result::success);
    }
}
