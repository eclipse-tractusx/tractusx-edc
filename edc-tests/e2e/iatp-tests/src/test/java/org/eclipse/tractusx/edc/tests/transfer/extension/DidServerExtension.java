/*
 * Copyright (c) 2025 Cofinity-X
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

package org.eclipse.tractusx.edc.tests.transfer.extension;

import org.eclipse.edc.connector.controlplane.test.system.utils.LazySupplier;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.util.io.Ports;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import static org.mockserver.model.HttpRequest.request;

/**
 * Centralized server for tests that exposes DIDs for participant.
 * Usually it should be the participants that expose the DID by themselves, but in our E2E tests this simplify things out.
 */
public class DidServerExtension implements BeforeAllCallback, AfterAllCallback {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LazySupplier<Integer> port = new LazySupplier<>(Ports::getFreePort);
    private ClientAndServer server;

    @Override
    public void beforeAll(ExtensionContext context) {
        server = ClientAndServer.startClientAndServer(port.get());
    }

    @Override
    public void afterAll(ExtensionContext context) {
        if (server != null) {
            server.stop();
        }
    }

    public DidServerExtension register(String name, DidDocument didDocument) {
        try {
            server.when(request("/%s/.well-known/did.json".formatted(name.toLowerCase())))
                    .respond(HttpResponse.response(objectMapper.writeValueAsString(didDocument)));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public String didFor(String name) {
        return "did:web:localhost%%3A%d%%2f%s".formatted(port.get(), name.toLowerCase());
    }
}
