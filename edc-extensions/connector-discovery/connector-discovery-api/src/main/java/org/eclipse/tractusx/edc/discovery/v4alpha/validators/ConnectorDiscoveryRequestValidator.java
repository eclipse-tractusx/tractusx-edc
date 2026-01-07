/*
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
 */

package org.eclipse.tractusx.edc.discovery.v4alpha.validators;

import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryValue;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import static java.lang.String.format;
import static org.eclipse.edc.validator.spi.Violation.violation;
import static org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorDiscoveryRequest.CONNECTOR_DISCOVERY_REQUEST_IDENTIFIER_ATTRIBUTE;
import static org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorDiscoveryRequest.CONNECTOR_DISCOVERY_REQUEST_KNOWNS_ATTRIBUTE;

public class ConnectorDiscoveryRequestValidator {
    public static Validator<JsonObject> instance() {
        return JsonObjectValidator.newValidator()
                .verify(CONNECTOR_DISCOVERY_REQUEST_IDENTIFIER_ATTRIBUTE, MandatoryValue::new)
                .verify(CONNECTOR_DISCOVERY_REQUEST_KNOWNS_ATTRIBUTE, KnownsValue::new)
                .build();
    }

    private static class KnownsValue implements Validator<JsonObject> {
        private final JsonLdPath path;

        public KnownsValue(JsonLdPath path) {
            this.path = path;
        }

        @Override
        public ValidationResult validate(JsonObject input) {
            var providedObject = input.getJsonArray(path.last());
            if (providedObject == null) {
                return ValidationResult.success();
            }

            var issues = new ArrayList<String>();
            for (JsonValue value : providedObject) {
                if (value.getValueType() != JsonValue.ValueType.STRING) {
                    issues.add(format("value '%s' is not of type STRING, it is of type %s", value.toString(), value.getValueType()));
                    continue;
                }
                var content = ((JsonString) value).getString();
                try {
                    new URL(content);
                } catch (MalformedURLException e) {
                    issues.add(format("value '%s' is not a valid url", content));
                    continue;
                }
            }

            if (!issues.isEmpty()) {
                return ValidationResult.failure(violation(format("Validation issues for field '%s': %s", path, issues), path.toString()));
            }

            return ValidationResult.success();
        }
    }

}
