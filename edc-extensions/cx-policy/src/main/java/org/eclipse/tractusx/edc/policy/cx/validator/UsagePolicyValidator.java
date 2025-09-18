/********************************************************************************
 * Copyright (c) 2025 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
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

import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OBLIGATION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PERMISSION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PROHIBITION_ATTRIBUTE;
import static org.eclipse.edc.validator.spi.Violation.violation;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.ACTION_USAGE;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.FRAMEWORK_AGREEMENT_LITERAL;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.USAGE_PURPOSE_LITERAL;

/**
 * Validates usage policy constraints according to the ODRL specification.
 * Ensures that usage policies contain at least one rule (permission, obligation, or prohibition)
 * and validates constraints for each rule type.
 */
public class UsagePolicyValidator implements Validator<JsonObject> {
    private final JsonLdPath path;
    private final Set<String> encounteredPermissionConstraints;
    private final Set<String> encounteredObligationConstraints;
    private final Set<String> encounteredProhibitionConstraints;
    private final Set<String> requiredPermissionConstraints = Set.of(
            FRAMEWORK_AGREEMENT_LITERAL,
            USAGE_PURPOSE_LITERAL
    );

    public UsagePolicyValidator(JsonLdPath path) {
        this.path = path;
        this.encounteredPermissionConstraints = new HashSet<>();
        this.encounteredObligationConstraints = new HashSet<>();
        this.encounteredProhibitionConstraints = new HashSet<>();
    }


    @Override
    public ValidationResult validate(JsonObject input) {
        if (LegacyPolicyCheck.isLegacy(input)) {
            return ValidationResult.success();
        }
        
        return JsonObjectValidator.newValidator()
                .verify(AtLeastOneRuleExists::new)
                .verifyArrayItem(ODRL_PERMISSION_ATTRIBUTE, builder ->
                        UsagePermissionValidator.instance(builder, encounteredPermissionConstraints))
                .verify(path -> new PermissionContainsRequiredConstraints(path, encounteredPermissionConstraints, requiredPermissionConstraints))
                .verifyArrayItem(ODRL_OBLIGATION_ATTRIBUTE, builder ->
                        UsageObligationValidator.instance(builder, encounteredObligationConstraints))
                .verifyArrayItem(ODRL_PROHIBITION_ATTRIBUTE, builder ->
                        UsageProhibitionValidator.instance(builder, encounteredProhibitionConstraints))
                .build()
                .validate(input);
    }

    private static final class PermissionContainsRequiredConstraints implements Validator<JsonObject> {
        private final JsonLdPath path;
        private final Set<String> encounteredConstraints;
        private final Set<String> requiredConstraints;

        PermissionContainsRequiredConstraints(JsonLdPath path, Set<String> encounteredConstraints, Set<String> requiredConstraints) {
            this.path = path;
            this.encounteredConstraints = encounteredConstraints;
            this.requiredConstraints = requiredConstraints;
        }

        @Override
        public ValidationResult validate(JsonObject input) {
            return requiredConstraints.stream()
                    .filter(constraint -> !encounteredConstraints.contains(constraint))
                    .findFirst()
                    .map(c -> ValidationResult.failure(
                            violation(String.format("Usage policy permission must include at least the following constraints %s", requiredConstraints.toString()), path.toString())))
                    .orElse(ValidationResult.success());
        }
    }


    private static final class UsagePermissionValidator {
        public static JsonObjectValidator.Builder instance(JsonObjectValidator.Builder builder, Set<String> encounteredConstraints) {

            return builder
                    .verify(path -> new ActionTypeIs(path, ACTION_USAGE))
                    .verifyArrayItem(ODRL_CONSTRAINT_ATTRIBUTE, b -> ConstraintValidator.instance(b, ACTION_USAGE, ODRL_PERMISSION_ATTRIBUTE, encounteredConstraints));
        }
    }

    private static final class UsageProhibitionValidator {
        public static JsonObjectValidator.Builder instance(JsonObjectValidator.Builder builder, Set<String> encounteredConstraints) {
            return builder
                    .verify(path -> new ActionTypeIs(path, ACTION_USAGE))
                    .verifyArrayItem(ODRL_CONSTRAINT_ATTRIBUTE, b -> ConstraintValidator.instance(b, ACTION_USAGE, ODRL_PROHIBITION_ATTRIBUTE, encounteredConstraints));
        }
    }

    private static final class UsageObligationValidator {
        public static JsonObjectValidator.Builder instance(JsonObjectValidator.Builder builder, Set<String> encounteredConstraints) {
            return builder
                    .verify(path -> new ActionTypeIs(path, ACTION_USAGE))
                    .verifyArrayItem(ODRL_CONSTRAINT_ATTRIBUTE, b -> ConstraintValidator.instance(b, ACTION_USAGE, ODRL_OBLIGATION_ATTRIBUTE, encounteredConstraints));
        }
    }
}
