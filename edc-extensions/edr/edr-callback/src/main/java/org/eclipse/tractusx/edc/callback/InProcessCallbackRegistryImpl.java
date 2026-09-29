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

import org.eclipse.edc.connector.controlplane.services.spi.callback.CallbackEventRemoteMessage;
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
