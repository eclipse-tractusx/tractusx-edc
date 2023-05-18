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

package org.eclipse.tractusx.edc.spi.cp.adapter.callback;


import org.eclipse.edc.connector.spi.callback.CallbackEventRemoteMessage;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.result.Result;


/**
 * Registry for {@link InProcessCallback}
 */
@ExtensionPoint
public interface InProcessCallbackRegistry {

    /**
     * Register an {@link InProcessCallback}
     *
     * @param callback The callback
     */
    void registerHandler(InProcessCallback callback);

    /**
     * Handles a {@link CallbackEventRemoteMessage} by calling registered {@link InProcessCallback}
     *
     * @param message The message
     */
    <T extends Event> Result<Void> handleMessage(CallbackEventRemoteMessage<T> message);
}
