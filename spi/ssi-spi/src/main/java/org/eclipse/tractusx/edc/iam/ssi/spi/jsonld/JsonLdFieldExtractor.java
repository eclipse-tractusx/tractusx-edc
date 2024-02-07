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

package org.eclipse.tractusx.edc.iam.ssi.spi.jsonld;

import jakarta.json.JsonObject;
import org.eclipse.edc.spi.result.Result;

import java.util.Objects;

import static jakarta.json.JsonValue.ValueType.ARRAY;
import static jakarta.json.JsonValue.ValueType.OBJECT;
import static java.lang.String.format;

/**
 * Extractor for field from a {@link JsonObject} with a customizable error reporting
 */
public class JsonLdFieldExtractor {

    private String fieldAlias;
    private String errorPrefix = "";
    private String field;

    private JsonLdFieldExtractor() {
    }

    /**
     * Extract a field by name. If not found return an error.
     */
    public Result<JsonObject> extract(JsonObject root) {
        var subjectArray = root.get(field);
        if (subjectArray == null || subjectArray.getValueType() != ARRAY) {
            return Result.failure(errorPrefix + format(" no %s found", fieldAlias));
        }
        if (subjectArray.asJsonArray().size() != 1) {
            return Result.failure(errorPrefix + format(" empty %s", fieldAlias));
        }

        var subjectValue = subjectArray.asJsonArray().get(0);
        if (subjectValue == null || subjectValue.getValueType() != OBJECT) {
            return Result.failure(errorPrefix + format(" invalid %s format", fieldAlias));
        }
        return Result.success(subjectValue.asJsonObject());
    }

    public static class Builder {

        private final JsonLdFieldExtractor extractor;

        private Builder(JsonLdFieldExtractor extractor) {
            this.extractor = extractor;
        }

        public static Builder newInstance() {
            return new Builder(new JsonLdFieldExtractor());
        }

        public Builder field(String field) {
            this.extractor.field = field;
            return this;
        }

        public Builder fieldAlias(String fieldAlias) {
            this.extractor.fieldAlias = fieldAlias;
            return this;
        }

        public Builder errorPrefix(String errorPrefix) {
            this.extractor.errorPrefix = errorPrefix;
            return this;
        }

        public JsonLdFieldExtractor build() {
            Objects.requireNonNull(extractor.field);
            Objects.requireNonNull(extractor.fieldAlias);
            Objects.requireNonNull(extractor.errorPrefix);
            return extractor;
        }

    }

}
