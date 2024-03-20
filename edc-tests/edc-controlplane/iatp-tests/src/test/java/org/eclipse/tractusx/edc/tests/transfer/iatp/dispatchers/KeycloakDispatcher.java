/*
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft
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
 */

package org.eclipse.tractusx.edc.tests.transfer.iatp.dispatchers;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.eclipse.edc.spi.types.TypeManager;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class KeycloakDispatcher extends Dispatcher {

    private static final TypeManager MAPPER = new TypeManager();
    private final String path;

    public KeycloakDispatcher(String path) {
        this.path = path;
    }

    public KeycloakDispatcher() {
        this("/");
    }

    @NotNull
    @Override
    public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) {
        if (recordedRequest.getPath().split("\\?")[0].equals(path)) {
            return createTokenResponse();
        }
        return new MockResponse().setResponseCode(404);
    }

    private MockResponse createTokenResponse() {
        return new MockResponse().setBody(MAPPER.writeValueAsString(Map.of("access_token", "token")));
    }

}
