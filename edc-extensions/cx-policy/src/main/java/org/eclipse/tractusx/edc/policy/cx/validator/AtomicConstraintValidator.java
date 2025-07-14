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
import org.eclipse.edc.validator.jsonobject.validators.MandatoryValue;
import org.eclipse.edc.validator.jsonobject.validators.OptionalIdNotBlank;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_LEFT_OPERAND_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OPERATOR_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_RIGHT_OPERAND_ATTRIBUTE;

/**
 * Validator for atomic constraints, enforces the presence and structure
 * of left operand, operator, and right operand components.
 * <p>
 * This validator ensures that:
 * <ul>
 *   <li>The left operand is present and valid according to policy and rule type constraints</li>
 *   <li>The operator is present with a valid identifier</li>
 *   <li>The right operand is present with a valid value</li>
 * </ul>
 * <p>
 * The validator is configured with policy type and rule type to apply appropriate
 * validation rules for different policy contexts.
 */
public class AtomicConstraintValidator implements Validator<JsonObject> {
    private final JsonLdPath path;
    private final String policyType;
    private final String ruleType;

    private AtomicConstraintValidator(JsonLdPath path, String policyType, String ruleType) {
        this.path = path;
        this.policyType = policyType;
        this.ruleType = ruleType;
    }

    public static AtomicConstraintValidator instance(JsonLdPath path, String policyType, String ruleType) {
        return new AtomicConstraintValidator(path, policyType, ruleType);
    }

    @Override
    public ValidationResult validate(JsonObject input) {
        return JsonObjectValidator.newValidator()
                .verify(ODRL_LEFT_OPERAND_ATTRIBUTE, MandatoryObject::new)
                .verifyObject(ODRL_LEFT_OPERAND_ATTRIBUTE, b -> LeftOperandValidator.instance(b, policyType, ruleType))
                .verify(ODRL_OPERATOR_ATTRIBUTE, MandatoryObject::new)
                .verifyObject(ODRL_OPERATOR_ATTRIBUTE, b -> b.verifyId(OptionalIdNotBlank::new))
                .verify(ODRL_RIGHT_OPERAND_ATTRIBUTE, MandatoryValue::new)
                .build()
                .validate(input);
    }
}