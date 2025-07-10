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

import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OBLIGATION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PERMISSION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PROHIBITION_ATTRIBUTE;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.ACTION_USAGE;

/**
 * Validates usage policy constraints according to the ODRL specification.
 * Ensures that usage policies contain at least one rule (permission, obligation, or prohibition)
 * and validates constraints for each rule type.
 */
public class UsagePolicyValidator implements Validator<JsonObject> {
    private final JsonLdPath path;

    public UsagePolicyValidator(JsonLdPath path) {
        this.path = path;
    }


    @Override
    public ValidationResult validate(JsonObject input) {
        var typeValidator = typeValidator(input);
        if (typeValidator.failed()) {
            return typeValidator;
        }
        return JsonObjectValidator.newValidator()
                .verify(AtLeastOneRuleExists::new)
                .verifyArrayItem(ODRL_PERMISSION_ATTRIBUTE, UsagePermissionValidator::instance)
                .verifyArrayItem(ODRL_OBLIGATION_ATTRIBUTE, UsageObligationValidator::instance)
                .verifyArrayItem(ODRL_PROHIBITION_ATTRIBUTE, UsageProhibitionValidator::instance)
                .build()
                .validate(input);
    }
    private ValidationResult typeValidator(JsonObject input) {
        return JsonObjectValidator.newValidator()
                .verify(AtLeastOneRuleExists::new)
                .verify(ODRL_PERMISSION_ATTRIBUTE, TypedMandatoryArray.orAbsent())
                .verify(ODRL_PROHIBITION_ATTRIBUTE, TypedMandatoryArray.orAbsent())
                .verify(ODRL_OBLIGATION_ATTRIBUTE, TypedMandatoryArray.orAbsent())
                .build()
                .validate(input);
    }

    private static final class UsagePermissionValidator {
        public static JsonObjectValidator.Builder instance(JsonObjectValidator.Builder builder) {
            return builder
                    .verify(path -> new ActionTypeIs(path, ACTION_USAGE))
                    .verifyArrayItem(ODRL_CONSTRAINT_ATTRIBUTE, b -> ConstraintValidator.instance(b, ACTION_USAGE, ODRL_PERMISSION_ATTRIBUTE));
        }
    }

    private static final class UsageProhibitionValidator {
        public static JsonObjectValidator.Builder instance(JsonObjectValidator.Builder builder) {
            return builder
                    .verify(path -> new ActionTypeIs(path, ACTION_USAGE))
                    .verifyArrayItem(ODRL_CONSTRAINT_ATTRIBUTE, b -> ConstraintValidator.instance(b, ACTION_USAGE, ODRL_PROHIBITION_ATTRIBUTE));
        }
    }

    private static final class UsageObligationValidator {
        public static JsonObjectValidator.Builder instance(JsonObjectValidator.Builder builder) {
            return builder
                    .verify(path -> new ActionTypeIs(path, ACTION_USAGE))
                    .verifyArrayItem(ODRL_CONSTRAINT_ATTRIBUTE, b -> ConstraintValidator.instance(b, ACTION_USAGE, ODRL_OBLIGATION_ATTRIBUTE));
        }
    }
}
