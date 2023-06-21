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

package org.eclipse.tractusx.edc.iam.ssi.spi.jsonld;

import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.document.JsonDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsonp.JSONPModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.CredentialsNamespaces.W3C_VC_NS;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.W3cVcContext.W3C_VC_CONTEXT;

/**
 * Test helpers for processing Json-Ld.
 */
public class JsonLdTextFixtures {

    /**
     * Creates a mapper configured to support Json-Ld processing.
     */
    public static ObjectMapper createObjectMapper() {
        var mapper = new ObjectMapper();
        mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new JSONPModule());
        var module = new SimpleModule() {
            @Override
            public void setupModule(SetupContext context) {
                super.setupModule(context);
            }
        };
        mapper.registerModule(module);
        return mapper;
    }

    /**
     * Performs Json-Ld compaction on an object.
     */
    public static JsonObject compact(JsonObject json) {
        try {
            var document = JsonDocument.of(json);
            var jsonFactory = Json.createBuilderFactory(Map.of());
            var contextDocument = JsonDocument.of(jsonFactory.createObjectBuilder().build());
            return com.apicatalog.jsonld.JsonLd.compact(document, contextDocument).get();
        } catch (JsonLdError e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Expands the document using the provided cache for resolving referenced contexts. The {@link CredentialsNamespaces#W3C_VC_NS} context is implicitly added to the cache.
     */
    public static JsonObject expand(JsonObject json, Map<String, String> contextCache) {
        var map = new HashMap<>(contextCache);
        map.put(W3C_VC_NS, W3C_VC_CONTEXT);
        try {
            var document = JsonDocument.of(json);
            var options = new JsonLdOptions((url, options1) -> JsonDocument.of(new StringReader(map.get(url.toString()))));
            var expanded = com.apicatalog.jsonld.JsonLd.expand(document).options(options).get();
            if (expanded.size() > 0) {
                return expanded.getJsonObject(0);
            }
            return Json.createObjectBuilder().build();
        } catch (JsonLdError e) {
            throw new AssertionError(e);
        }
    }
}
