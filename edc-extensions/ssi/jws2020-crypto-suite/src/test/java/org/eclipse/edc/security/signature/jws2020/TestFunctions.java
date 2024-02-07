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

package org.eclipse.edc.security.signature.jws2020;

import com.apicatalog.ld.signature.key.KeyPair;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWK;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.UUID;

class TestFunctions {

    private static final ObjectMapper MAPPER = JacksonJsonLd.createObjectMapper();

    static KeyPair createKeyPair(JWK jwk) {
        var id = URI.create("https://org.eclipse.tractusx/keys/" + UUID.randomUUID());
        var type = URI.create("https://w3id.org/security#JsonWebKey2020");
        return new JwkMethod(id, type, null, jwk);
    }

    static JsonObject readResourceAsJson(String name) {
        try {
            return MAPPER.readValue(Thread.currentThread().getContextClassLoader().getResourceAsStream(name), JsonObject.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static String readResourceAsString(String name) {
        try (var stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(name)) {
            return new String(Objects.requireNonNull(stream).readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
