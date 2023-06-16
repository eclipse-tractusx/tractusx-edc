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

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;

/**
 * Provides functions for working with Json-Ld types.
 */
public class JsonLdTypeFunctions {
    private static final String TYPE = "@type";
    private static final Stream<JsonObject> EMPTY_STREAM = Stream.of();

    private JsonLdTypeFunctions() {
    }

    /**
     * Returns a stream of objects that are of the given Json-Ld type starting at the root.
     *
     * @param typeValue the type to include
     * @param root      the root object to traverse
     * @return the stream of types
     */
    public static Stream<JsonObject> extractObjectsOfType(String typeValue, JsonStructure root) {
        if (root instanceof JsonObject rootObject) {
            return matchTypeValue(typeValue, rootObject.get(TYPE)) ? Stream.of(rootObject) :
                    extractObjectsOfType(typeValue, rootObject.values().stream());
        } else if (root instanceof JsonArray rootArray) {
            return extractObjectsOfType(typeValue, rootArray.stream());
        }
        return EMPTY_STREAM;
    }

    /**
     * Returns a stream of objects that are of the given Json-Ld type in the stream.
     *
     * @param typeValue the type to include
     * @param stream    the stream of roots to traverse
     * @return the stream of types
     */
    public static Stream<JsonObject> extractObjectsOfType(String typeValue, Stream<JsonValue> stream) {
        return stream.filter(v -> v instanceof JsonStructure)
                .flatMap(v -> extractObjectsOfType(typeValue, (JsonStructure) v)).filter(Objects::nonNull);
    }

    /**
     * Partitions a stream of objects by their type, returning a type-to-collection mapping.
     */
    public static Map<String, List<JsonObject>> partitionByType(Stream<JsonObject> stream) {
        var partitions = new HashMap<String, List<JsonObject>>();
        stream.forEach(object -> getTypes(object).forEach(type -> partitions.computeIfAbsent(type, k -> new ArrayList<>()).add(object)));
        return partitions;
    }

    /**
     * Returns the types associated with the object
     */
    private static Set<String> getTypes(JsonObject object) {
        var result = object.get(TYPE);
        if (result instanceof JsonArray resultArray) {
            return resultArray.stream().filter(e -> e instanceof JsonString).map(s -> ((JsonString) s).getString()).collect(toSet());
        } else if (result instanceof JsonString resultString) {
            return Set.of(resultString.getString());
        }
        return emptySet();
    }

    /**
     * Returns true if the type value matches the Json value.
     */
    private static boolean matchTypeValue(String typeValue, JsonValue jsonValue) {
        if (jsonValue instanceof JsonString stringValue) {
            return typeValue.equals(stringValue.getString());
        } else if (jsonValue instanceof JsonArray arrayValue) {
            return arrayValue.stream().anyMatch(v -> v instanceof JsonString && typeValue.equals(((JsonString) v).getString()));
        }
        return false;
    }
}
