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
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.jetbrains.annotations.Nullable;

import static jakarta.json.JsonValue.ValueType;

/**
 * Functions for working with Json-ld values.
 */
public class JsonLdValueFunctions {
    private static final String VALUE = "@value";

    private JsonLdValueFunctions() {
    }

    /**
     * Extracts the value of a root node and converts it to a string representation. Note this method accepts null nodes as a convenience.
     */
    @Nullable
    public static String extractStringValue(@Nullable JsonValue root) {
        if (root == null) {
            return null;
        } else if (root instanceof JsonArray rootArray) {
            if (rootArray.isEmpty()) {
                return null;
            }
            var jsonValue = rootArray.get(0);
            return (jsonValue instanceof JsonObject elementObject) ? convertType(elementObject.get(VALUE)) : null;
        } else if (root instanceof JsonObject rootObject) {
            return convertType(rootObject.get(VALUE));
        } else {
            return convertType(root);
        }
    }

    /**
     * Converts the value to a string representation.
     */
    @Nullable
    private static String convertType(JsonValue value) {
        if (value instanceof JsonString valueString) {
            return valueString.getString();
        } else if (value instanceof JsonNumber valueNumber) {
            return valueNumber.isIntegral() ? String.valueOf(valueNumber.longValue()) : String.valueOf(valueNumber.doubleValue());
        } else if (ValueType.TRUE == value.getValueType()) {
            return "TRUE";
        } else if (ValueType.FALSE == value.getValueType()) {
            return "FALSE";
        }
        return null;
    }
}
