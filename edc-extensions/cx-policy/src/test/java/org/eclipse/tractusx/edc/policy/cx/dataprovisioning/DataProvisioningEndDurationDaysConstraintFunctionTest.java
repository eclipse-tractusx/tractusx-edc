/********************************************************************************
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.edc.policy.cx.dataprovisioning;

import org.eclipse.edc.connector.controlplane.contract.spi.policy.AgreementPolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.tractusx.edc.policy.cx.TestAgreementPolicyContext;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class DataProvisioningEndDurationDaysConstraintFunctionTest {

    private final DataProvisioningEndDurationDaysConstraintFunction<AgreementPolicyContext> function = new DataProvisioningEndDurationDaysConstraintFunction<>();

    @Test
    void evaluate_whenPolicyIsValid_thenTrue() {
        var validContext = new TestAgreementPolicyContext();
        var result = function.evaluate(Operator.EQ, 1, null, validContext);
        assertThat(result).isTrue();
    }

    @Test
    void evaluate_whenPolicyIsInSameDay_thenFalse() {
        var expiredContext = new TestAgreementPolicyContext();
        var result = function.evaluate(Operator.EQ, 0, null, expiredContext);
        assertThat(result).isFalse();
    }

    @Test
    void evaluate_whenPolicyIsExpired_thenFalse() {
        var expiredContext = new TestAgreementPolicyContext(Instant.now().minus(1, ChronoUnit.DAYS));
        var result = function.evaluate(Operator.EQ, -1, null, expiredContext);
        assertThat(result).isFalse();
    }

    @Test
    void validate_whenOperatorAndRightOperandAreValid_thenSuccess() {
        var result = function.validate(Operator.EQ, 1, null);
        assertThat(result).isSucceeded();
    }

    @Test
    void validate_whenOperatorAndRightOperandAreValidString_thenSuccess() {
        var result = function.validate(Operator.EQ, "1", null);
        assertThat(result).isSucceeded();
    }

    @Test
    void validate_whenInvalidOperator_thenFailure() {
        var result = function.validate(Operator.IS_ANY_OF, 1, null);
        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureDetail()).contains("Invalid operator");
    }

    @Test
    void validate_whenInvalidValue_thenFailure() {
        var result = function.validate(Operator.EQ, "invalid-test", null);
        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureDetail()).contains("Invalid right-operand: ");
    }
}
