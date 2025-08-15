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
import org.eclipse.edc.validator.jsonobject.validators.MandatoryIdNotBlank;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import java.util.HashSet;
import java.util.Set;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OBLIGATION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PERMISSION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PROHIBITION_ATTRIBUTE;
import static org.eclipse.edc.validator.spi.Violation.violation;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.ACCESS_OBLIGATION_POLICY_ALLOWED_LEFT_OPERANDS;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.ACCESS_PERMISSION_POLICY_ALLOWED_LEFT_OPERANDS;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.ACCESS_PROHIBITION_POLICY_ALLOWED_LEFT_OPERANDS;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.ACTION_ACCESS;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.MUTUALLY_EXCLUSIVE_CONSTRAINTS;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.USAGE_OBLIGATION_POLICY_ALLOWED_LEFT_OPERANDS;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.USAGE_PERMISSION_POLICY_ALLOWED_LEFT_OPERANDS;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.USAGE_PROHIBITION_POLICY_ALLOWED_LEFT_OPERANDS;


/**
 * Validator for left operands that enforces allowed operand values
 * based on policy type and rule type combinations.
 * <p>
 * This validator ensures that:
 * <ul>
 *   <li>The left operand has a mandatory, non-blank ID</li>
 *   <li>The operand value is within the allowed set for the specific policy and rule type</li>
 * </ul>
 * <p>
 * The validator is configured with policy type and rule type to determine the appropriate
 * set of allowed left operand values for validation.
 */
public class LeftOperandValidator implements Validator<JsonObject> {
    private final JsonLdPath path;
    private final String policyType;
    private final String ruleType;
    private final Set<String> encounteredConstraints;

    private LeftOperandValidator(JsonLdPath path, String policyType, String ruleType, Set<String> encounteredConstraints) {
        this.path = path;
        this.policyType = policyType;
        this.ruleType = ruleType;
        this.encounteredConstraints = encounteredConstraints;
    }

    public static JsonObjectValidator.Builder instance(JsonObjectValidator.Builder builder, String policyType, String ruleType, Set<String> leftOperands) {
        return builder.verify(path -> new LeftOperandValidator(path, policyType, ruleType, leftOperands));
    }

    public static JsonObjectValidator.Builder instance(JsonObjectValidator.Builder builder, String policyType, String ruleType) {
        return builder.verify(path -> new LeftOperandValidator(path, policyType, ruleType, new HashSet<>()));
    }

    @Override
    public ValidationResult validate(JsonObject input) {
        return JsonObjectValidator.newValidator()
                .verifyId(MandatoryIdNotBlank::new)
                .verify(ID, path -> new ValueIn(path, getAllowedLeftOperands(), encounteredConstraints))
                .build()
                .validate(input);
    }

    @SuppressWarnings("checkstyle:RightCurly")
    private Set<String> getAllowedLeftOperands() {
        if (policyType.equals(ACTION_ACCESS)) {
            switch (ruleType) {
                case ODRL_PERMISSION_ATTRIBUTE -> {
                    return ACCESS_PERMISSION_POLICY_ALLOWED_LEFT_OPERANDS;
                }
                case ODRL_PROHIBITION_ATTRIBUTE -> {
                    return ACCESS_PROHIBITION_POLICY_ALLOWED_LEFT_OPERANDS;
                }
                case ODRL_OBLIGATION_ATTRIBUTE -> {
                    return ACCESS_OBLIGATION_POLICY_ALLOWED_LEFT_OPERANDS;
                }
                default -> {
                    return Set.of();
                }
            }
        } else {
            switch (ruleType) {
                case ODRL_PERMISSION_ATTRIBUTE -> {
                    return USAGE_PERMISSION_POLICY_ALLOWED_LEFT_OPERANDS;
                }
                case ODRL_PROHIBITION_ATTRIBUTE -> {
                    return USAGE_PROHIBITION_POLICY_ALLOWED_LEFT_OPERANDS;
                }
                case ODRL_OBLIGATION_ATTRIBUTE -> {
                    return USAGE_OBLIGATION_POLICY_ALLOWED_LEFT_OPERANDS;
                }
                default -> {
                    return Set.of();
                }
            }
        }
    }

    private static final class ValueIn implements Validator<JsonObject> {
        private final Set<String> allowedValues;
        private final JsonLdPath path;
        private final String id;
        private final Set<String> encounteredConstraints;

        private ValueIn(JsonLdPath path, Set<String> allowedValues, Set<String> encounteredConstraints) {
            this.path = path;
            this.allowedValues = allowedValues;
            this.id = path.last();
            this.encounteredConstraints = encounteredConstraints;
        }

        @Override
        public ValidationResult validate(JsonObject input) {
            var value = input.getString(id, null);
            if (value == null || value.isBlank() || value.substring(value.lastIndexOf("/") + 1).isBlank()) {
                return ValidationResult.failure(
                        violation("leftOperand.'" + id + "' value can not be null or empty. Expected one of: " + allowedValues, path.toString())
                );
            }
            if (!allowedValues.contains(value)) {
                return ValidationResult.failure(
                        violation("leftOperand.@id value '" + value + "' is not allowed. Expected one of: " + allowedValues, path.toString())
                );
            }
            var hasMutualExclusionConflict = hasMutualExclusionConflict(value);
            if (hasMutualExclusionConflict.failed()) {
                return hasMutualExclusionConflict;
            }
            if (encounteredConstraints != null) {
                encounteredConstraints.add(value);
            }

            return ValidationResult.success();
        }

        private ValidationResult hasMutualExclusionConflict(String leftOperand) {
            Set<String> mutuallyExclusiveWithCurrent = MUTUALLY_EXCLUSIVE_CONSTRAINTS.stream()
                    .filter(mutuallyExclusive -> mutuallyExclusive.contains(leftOperand))
                    .findFirst()
                    .map(mutuallyExclusive -> {
                        Set<String> exclusiveSet = new HashSet<>(mutuallyExclusive);
                        exclusiveSet.remove(leftOperand);
                        return exclusiveSet;
                    })
                    .orElse(new HashSet<>());

            for (Set<String> mutuallyExclusive : MUTUALLY_EXCLUSIVE_CONSTRAINTS) {
                if (mutuallyExclusive.contains(leftOperand)) {
                    mutuallyExclusiveWithCurrent = new HashSet<>(mutuallyExclusive);
                    mutuallyExclusiveWithCurrent.remove(leftOperand);
                    break;
                }
            }

            return encounteredConstraints.stream()
                    .filter(mutuallyExclusiveWithCurrent::contains)
                    .findFirst()
                    .map(conflictingKey -> {
                        return ValidationResult.failure(
                                violation(String.format("Constraint '%s' is mutually exclusive with constraint '%s'.", leftOperand, conflictingKey), path.toString()));
                    })
                    .orElse(ValidationResult.success());
        }
    }
}
