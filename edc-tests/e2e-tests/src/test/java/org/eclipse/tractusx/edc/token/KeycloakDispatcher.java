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

package org.eclipse.tractusx.edc.token;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.eclipse.edc.spi.types.TypeManager;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class KeycloakDispatcher extends Dispatcher {

    private static final TypeManager MAPPER = new TypeManager();
    
    public KeycloakDispatcher() {

    }

    @NotNull
    @Override
    public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) throws InterruptedException {
        if (recordedRequest.getPath().split("\\?")[0].equals("/")) {
            return createTokenResponse();
        }
        return new MockResponse().setResponseCode(404);
    }

    private MockResponse createTokenResponse() {
        return new MockResponse().setBody(MAPPER.writeValueAsString(Map.of("access_token", "token")));
    }

}
