/********************************************************************************
 * Copyright (c) 2025 Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
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

package org.eclipse.tractusx.edc.policy.cx.validator;

import jakarta.json.JsonObject;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryObject;
import org.eclipse.edc.validator.jsonobject.validators.OptionalIdNotBlank;
import org.eclipse.edc.validator.jsonobject.validators.TypeIs;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import java.util.Map;
import java.util.function.Function;

import static org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition.EDC_POLICY_DEFINITION_POLICY;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_POLICY_TYPE_SET;
import static org.eclipse.edc.validator.spi.Violation.violation;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.ACCESS_POLICY_TYPE;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.USAGE_POLICY_TYPE;

/**
 * Validates Catena-X policy constraints according to the specification.
 */
public class CxPolicyDefinitionValidator {

    private static final Map<String, Function<JsonLdPath, Validator<JsonObject>>> POLICY_VALIDATORS = Map.of(
            ACCESS_POLICY_TYPE, AccessPolicyValidator::new,
            USAGE_POLICY_TYPE, UsagePolicyValidator::new
    );

    /**
     * Creates a validator for Catena-X policy definitions.
     * <p>
     * The validator performs the following checks:
     * <ul>
     *   <li>Validates optional ID is not blank if present</li>
     *   <li>Ensures policy object is present and mandatory</li>
     *   <li>Validates policy type and applies appropriate policy-specific validation</li>
     *   <li>Supports ACCESS and USAGE policy types with their respective constraint rules</li>
     * </ul>
     *
     * @return a validator instance for JSON object policy definitions
     */
    public static Validator<JsonObject> instance() {
        return JsonObjectValidator.newValidator()
                .verifyId(OptionalIdNotBlank::new)
                .verify(EDC_POLICY_DEFINITION_POLICY, MandatoryObject::new)
                .verifyObject(EDC_POLICY_DEFINITION_POLICY, PolicyValidator::instance)
                .build();
    }

    private static final class PolicyValidator {
        public static JsonObjectValidator.Builder instance(JsonObjectValidator.Builder builder) {
            return builder
                    .verify(path -> new TypeIs(path, ODRL_POLICY_TYPE_SET))
                    .verify(PolicyTypeValidator::new);
        }
    }

    private record PolicyTypeValidator(JsonLdPath path) implements Validator<JsonObject> {
        @Override
        public ValidationResult validate(JsonObject input) {
            String policyType;
            try {
                policyType = PolicyTypeResolver.resolve(input);
            } catch (Exception e) {
                return ValidationResult.failure(
                        violation("Failed to resolve policy type: " + e.getMessage(), path.toString())
                );
            }
            var validatorFactory = POLICY_VALIDATORS.get(policyType);
            if (validatorFactory != null) {
                return validatorFactory.apply(path).validate(input);
            }
            return ValidationResult.failure(
                    violation("Policy type is not recognized. Expected one of: " + POLICY_VALIDATORS.keySet() + ", but got: " + policyType, path.toString())
            );
        }
    }
}