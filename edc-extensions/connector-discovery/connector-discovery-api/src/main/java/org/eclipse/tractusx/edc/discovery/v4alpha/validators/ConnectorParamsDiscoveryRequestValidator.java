/*
 * Copyright (c) 2025 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryValue;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import static org.eclipse.edc.validator.spi.Violation.violation;
import static org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorParamsDiscoveryRequest.DISCOVERY_PARAMS_REQUEST_COUNTER_PARTY_ADDRESS_ATTRIBUTE;
import static org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorParamsDiscoveryRequest.DISCOVERY_PARAMS_REQUEST_IDENTIFIER_ATTRIBUTE;
import static org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorParamsDiscoveryRequest.DISCOVERY_PARAMS_REQUEST_IDENTIFIER_ATTRIBUTE_LEGACY;

/**
 * Validator for the 'ConnectorParamsDiscoveryRequest' as defined in the connector discovery api.
 * <p>
 * The validator check the availability of the 'counterPartyAddress' field as this is mandatory. In addition,
 * it checks the availability of the 'counterPartyId' field which is mandatory as well. But for backward compatibility
 * it is also possible to use the field name 'bpnl' used earlier. The validator checks that at least one of the
 * two properties is defined and that they fulfil the requirements of the MandatoryValue validator.
 */
public class ConnectorParamsDiscoveryRequestValidator {

    public static Validator<JsonObject> instance() {
        return JsonObjectValidator.newValidator()
                .verify(DISCOVERY_PARAMS_REQUEST_IDENTIFIER_ATTRIBUTE, CounterPartyIdValidator::new)
                .verify(DISCOVERY_PARAMS_REQUEST_IDENTIFIER_ATTRIBUTE_LEGACY, BpnlValidator::new)
                .verify(DISCOVERY_PARAMS_REQUEST_COUNTER_PARTY_ADDRESS_ATTRIBUTE, MandatoryValue::new)
                .build();
    }

    private static class CounterPartyIdValidator implements Validator<JsonObject> {
        private final JsonLdPath path;

        CounterPartyIdValidator(JsonLdPath path) {
            this.path = path;
        }

        @Override
        public ValidationResult validate(JsonObject input) {
            var providedObject = input.getJsonArray(path.last());
            if (providedObject == null) {
                if (input.getJsonArray(DISCOVERY_PARAMS_REQUEST_IDENTIFIER_ATTRIBUTE_LEGACY) != null) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure(
                        violation("Neither '%s' nor '%s' property given"
                                .formatted(DISCOVERY_PARAMS_REQUEST_IDENTIFIER_ATTRIBUTE,
                                        DISCOVERY_PARAMS_REQUEST_IDENTIFIER_ATTRIBUTE_LEGACY),
                                path.toString()));
            }

            return new MandatoryValue(path).validate(input);
        }
    }

    private static class BpnlValidator implements Validator<JsonObject> {
        private final JsonLdPath path;

        BpnlValidator(JsonLdPath path) {
            this.path = path;
        }

        @Override
        public ValidationResult validate(JsonObject input) {
            var providedObject = input.getJsonArray(path.last());
            if (providedObject == null) {
                return ValidationResult.success();
            }

            return new MandatoryValue(path).validate(input);
        }
    }
}
