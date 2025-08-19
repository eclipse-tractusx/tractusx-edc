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

import java.util.HashSet;
import java.util.Set;

import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.ALLOWED_LOGICAL_CONSTRAINTS;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.NOT_ALLOWED_LOGICAL_CONSTRAINTS;

/**
 * Validator that routes constraint validation to either logical or atomic constraint validators
 * based on the constraint type detected in the JSON object.
 * <p>
 * This validator acts as a dispatcher that:
 * <ul>
 *   <li>Analyzes the constraint structure to determine if it's logical or atomic</li>
 *   <li>Routes logical constraints to {@link LogicalConstraintValidator}</li>
 *   <li>Routes atomic constraints to {@link AtomicConstraintValidator}</li>
 * </ul>
 * <p>
 * The validator is configured with policy type and rule type to pass appropriate
 * context to the underlying specialized validators.
 */
public class ConstraintValidator implements Validator<JsonObject> {
    private final JsonLdPath path;
    private final String policyType;
    private final String ruleType;
    private final Set<String> encounteredConstraints;

    private ConstraintValidator(JsonLdPath path, String policyType, String ruleType, Set<String> encounteredConstraints) {
        this.path = path;
        this.policyType = policyType;
        this.ruleType = ruleType;
        this.encounteredConstraints = encounteredConstraints;
    }

    public static JsonObjectValidator.Builder instance(JsonObjectValidator.Builder builder, String policyType, String ruleType, Set<String> encounteredConstraints) {
        return builder.verify(path -> new ConstraintValidator(path, policyType, ruleType, encounteredConstraints));
    }

    public static JsonObjectValidator.Builder instance(JsonObjectValidator.Builder builder, String policyType, String ruleType) {
        return builder.verify(path -> new ConstraintValidator(path, policyType, ruleType, new HashSet<>()));
    }

    @Override
    public ValidationResult validate(JsonObject input) {
        boolean isLogical = input.keySet().stream()
                .anyMatch(key -> ALLOWED_LOGICAL_CONSTRAINTS.contains(key) ||
                        NOT_ALLOWED_LOGICAL_CONSTRAINTS.contains(key));

        return isLogical ?
                LogicalConstraintValidator.instance(path, policyType, ruleType, encounteredConstraints).validate(input) :
                AtomicConstraintValidator.instance(path, policyType, ruleType, encounteredConstraints).validate(input);
    }
}
