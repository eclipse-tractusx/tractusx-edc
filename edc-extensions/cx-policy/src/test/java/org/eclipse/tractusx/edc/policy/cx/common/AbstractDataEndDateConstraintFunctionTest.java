/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.edc.policy.cx.common;

import org.eclipse.edc.connector.controlplane.contract.spi.policy.AgreementPolicyContext;
import org.eclipse.edc.junit.assertions.AbstractResultAssert;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.tractusx.edc.policy.cx.TestAgreementPolicyContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class AbstractDataEndDateConstraintFunctionTest {

    private final AbstractDataEndDateConstraintFunction<Permission, AgreementPolicyContext> function = new AbstractDataEndDateConstraintFunction<>() {};
    private final AgreementPolicyContext context = new TestAgreementPolicyContext();

    @Test
    void evaluate_whenPolicyIsValid_thenTrue() {
        var result = function.evaluate(
                Operator.EQ,
                context.now().plus(1, ChronoUnit.SECONDS).truncatedTo(ChronoUnit.SECONDS).toString(),
                null, context);
        assertThat(result).isTrue();
    }

    @Test
    void evaluate_whenPolicyIsSameTime_thenTrue() {
        var fixedNow = context.now().truncatedTo(ChronoUnit.SECONDS);
        // Ensure the Instant.now method called inside the evaluate function returns the fixedNow instant, to force equality comparison
        try (var mockedInstant = Mockito.mockStatic(Instant.class, Mockito.CALLS_REAL_METHODS)) {
            mockedInstant.when(Instant::now).thenReturn(fixedNow);
            var result = function.evaluate(
                    Operator.EQ,
                    fixedNow.toString(),
                    null, context);
            assertThat(result).isTrue();
        }
    }

    @Test
    void evaluate_whenPolicyIsExpired_thenFalse() {
        var result = function.evaluate(
                Operator.EQ,
                context.now().minus(1, ChronoUnit.SECONDS).truncatedTo(ChronoUnit.SECONDS).toString(),
                null, context);
        assertThat(result).isFalse();
    }

    @Test
    void validate_whenOperatorAndRightOperandAreValid_thenSuccess() {
        var result = function.validate(Operator.EQ, "2025-06-30T14:30:00Z", null);
        AbstractResultAssert.assertThat(result).isSucceeded();
    }

    @Test
    void validate_whenInvalidOperator_thenFailure() {
        var result = function.validate(Operator.IS_ANY_OF, "2025-06-30T14:30:00+01:00", null);
        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureDetail()).contains("Invalid operator");
    }

    @Test
    void validate_whenInvalidInstant_thenFailure() {
        var result = function.validate(Operator.EQ, "2025-06-30T14:30:00.456Z", null);
        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureDetail()).contains("Invalid right-operand: ");
    }

    @Test
    void validate_whenInvalidValue_thenFailure() {
        var result = function.validate(Operator.EQ, "invalid-test", null);
        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureDetail()).contains("Invalid right-operand: ");
    }
}
