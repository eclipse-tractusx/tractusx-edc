/********************************************************************************
 * Copyright (c) 2026 Cofinity-X GmbH
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

package org.eclipse.edc.jsonld;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.mock;

class TitaniumJsonLdTest {

    private static final String CONTEXT = """
            { "@context": { "test": "https://test.org/ns/", "name": "test:name" } }
            """;

    @TempDir
    private Path tempDir;
    private Path contextFile;

    private final TitaniumJsonLd jsonLd = new TitaniumJsonLd(mock(Monitor.class),
            JsonLdConfiguration.Builder.newInstance().httpEnabled(false).httpsEnabled(false).build());

    @BeforeEach
    void setUp() throws IOException {
        contextFile = tempDir.resolve("context.jsonld");
        Files.writeString(contextFile, CONTEXT);
    }

    @Test
    void expand_registeredFileContext_isResolvedFromCache() {
        jsonLd.registerCachedDocument("https://test.org/context", contextFile.toUri());

        var result = jsonLd.expand(document("https://test.org/context"));

        assertThat(result).isSucceeded().satisfies(expanded ->
                org.assertj.core.api.Assertions.assertThat(expanded).containsKey("https://test.org/ns/name"));
    }

    @Test
    void expand_unregisteredFileContext_isNotLoaded() {
        var result = jsonLd.expand(document(contextFile.toUri().toString()));

        assertThat(result).isFailed();
    }

    @Test
    void expand_unregisteredJarContext_isNotLoaded() {
        var result = jsonLd.expand(document("jar:" + contextFile.toUri() + "!/context.jsonld"));

        assertThat(result).isFailed();
    }

    @Test
    void expand_unregisteredHttpContext_isNotLoadedWhenHttpDisabled() {
        var result = jsonLd.expand(document("http://localhost:1/context"));

        assertThat(result).isFailed();
    }

    private JsonObject document(String context) {
        return Json.createObjectBuilder()
                .add("@context", context)
                .add("name", "value")
                .build();
    }
}
