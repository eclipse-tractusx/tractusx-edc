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
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.result.Result;

/**
 * In process callback for handling {@link CallbackEventRemoteMessage}
 */
@FunctionalInterface
public interface InProcessCallback {

    <T extends Event> Result<Void> invoke(CallbackEventRemoteMessage<T> message);
}
