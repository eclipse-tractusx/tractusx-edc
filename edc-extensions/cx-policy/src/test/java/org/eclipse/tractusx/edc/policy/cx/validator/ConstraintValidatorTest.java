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

import jakarta.json.Json;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_AND_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_AND_SEQUENCE_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OR_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PERMISSION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_XONE_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyBuilderFixtures.atomicConstraint;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyBuilderFixtures.logicalConstraint;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.ACTION_ACCESS;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.FRAMEWORK_AGREEMENT_LITERAL;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.MEMBERSHIP_LITERAL;

class ConstraintValidatorTest {

    private JsonObjectValidator accessPolicyValidator;

    @BeforeEach
    void setUp() {
        accessPolicyValidator = ConstraintValidator.instance(JsonObjectValidator.newValidator(), ACTION_ACCESS, ODRL_PERMISSION_ATTRIBUTE).build();
    }

    @Test
    void shouldReturnSuccess_whenValidAtomicConstraint() {
        var constraint = atomicConstraint(FRAMEWORK_AGREEMENT_LITERAL);

        ValidationResult result = accessPolicyValidator.validate(constraint);

        assertThat(result).isSucceeded();
    }

    @ParameterizedTest
    @ValueSource(strings = {ODRL_AND_CONSTRAINT_ATTRIBUTE})
    void shouldReturnSuccess_whenAllowedLogicalOperator(String operator) {
        var logicalConstraint = logicalConstraint(operator,
                atomicConstraint(FRAMEWORK_AGREEMENT_LITERAL),
                atomicConstraint(MEMBERSHIP_LITERAL));

        ValidationResult result = accessPolicyValidator.validate(logicalConstraint);

        assertThat(result).isSucceeded();
    }

    @ParameterizedTest
    @ValueSource(strings = {ODRL_OR_CONSTRAINT_ATTRIBUTE, ODRL_XONE_CONSTRAINT_ATTRIBUTE, ODRL_AND_SEQUENCE_CONSTRAINT_ATTRIBUTE})
    void shouldReturnFailure_whenNotAllowedLogicalOperator(String operator) {
        var logicalConstraint = logicalConstraint(operator,
                atomicConstraint(FRAMEWORK_AGREEMENT_LITERAL),
                atomicConstraint(MEMBERSHIP_LITERAL));

        ValidationResult result = accessPolicyValidator.validate(logicalConstraint);

        assertThat(result).isFailed();
    }

    @Test
    void shouldReturnFailure_whenEmptyConstraint() {
        var constraint = Json.createObjectBuilder().build();

        ValidationResult result = accessPolicyValidator.validate(constraint);

        assertThat(result).isFailed();
    }
}
