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
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import static org.eclipse.edc.validator.spi.Violation.violation;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.ALLOWED_LOGICAL_CONSTRAINTS;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.NOT_ALLOWED_LOGICAL_CONSTRAINTS;

/**
 * Validator for logical constraints, enforces allowed logical constraint
 * and validates structure.
 * <p>
 * This validator ensures that:
 * <ul>
 *   <li>Only allowed logical constraints are present (e.g., AND constraints)</li>
 *   <li>Exactly one logical constraint is specified per policy</li>
 *   <li>The logical constraint is properly formatted as an array</li>
 *   <li>Each constraint within the logical constraint is valid</li>
 * </ul>
 * <p>
 * The validator is configured with policy type and rule type to apply appropriate
 * validation rules for different policy contexts.
 */
public class LogicalConstraintValidator implements Validator<JsonObject> {
    private final JsonLdPath path;
    private final String policyType;
    private final String ruleType;

    private LogicalConstraintValidator(JsonLdPath path, String policyType, String ruleType) {
        this.path = path;
        this.policyType = policyType;
        this.ruleType = ruleType;
    }

    public static LogicalConstraintValidator instance(JsonLdPath path, String policyType, String ruleType) {
        return new LogicalConstraintValidator(path, policyType, ruleType);
    }

    @Override
    public ValidationResult validate(JsonObject input) {
        var invalidConstraintsResult = validateNoInvalidConstraints(input);
        if (invalidConstraintsResult.failed()) {
            return invalidConstraintsResult;
        }

        var logicalConstraints = input.keySet().stream()
                .filter(ALLOWED_LOGICAL_CONSTRAINTS::contains)
                .toList();
        if (logicalConstraints.size() != 1) {
            return ValidationResult.failure(
                    violation("Policy must have exactly one logical constraint: " + logicalConstraints,
                            path.toString())
            );
        }
        return validateLogicalConstraintContent(input, logicalConstraints.get(0));
    }

    private ValidationResult validateLogicalConstraintContent(JsonObject input, String logicalConstraint) {
        var arrayValidationResult = validateLogicalConstraintIsArray(input, logicalConstraint);
        if (arrayValidationResult.failed()) {
            return arrayValidationResult;
        }
        return JsonObjectValidator.newValidator()
                .verifyArrayItem(logicalConstraint, b -> ConstraintValidator.instance(b, policyType, ruleType))
                .build()
                .validate(input);
    }

    private ValidationResult validateLogicalConstraintIsArray(JsonObject input, String logicalConstraint) {
        return JsonObjectValidator.newValidator()
                .verify(logicalConstraint, TypedMandatoryArray::new)
                .build()
                .validate(input);
    }

    private ValidationResult validateNoInvalidConstraints(JsonObject input) {
        var invalidLogicalConstraints = input.keySet().stream()
                .filter(NOT_ALLOWED_LOGICAL_CONSTRAINTS::contains)
                .toList();

        if (!invalidLogicalConstraints.isEmpty()) {
            return ValidationResult.failure(
                    violation("Policy includes not allowed logical constraints: " + invalidLogicalConstraints, path.toString())
            );
        }
        return ValidationResult.success();
    }
}
