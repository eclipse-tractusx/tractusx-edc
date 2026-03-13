/*
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
 */

package org.eclipse.tractusx.edc.policy.cx.datausage;

import org.eclipse.edc.connector.controlplane.contract.spi.policy.AgreementPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.result.Result;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * This is a constraint function for DataUsageEndDate. It evaluates to true if the current date is before the specified
 * expiry date. The expiry date is expected to be in ISO-8061 UTC date-time format (e.g. "2024-12-31T23:59:59Z").
 */
public class DataUsageEndDateConstraintFunction<C extends AgreementPolicyContext> implements AtomicConstraintRuleFunction<Permission, C> {
    public static final String DATA_USAGE_END_DATE = "DataUsageEndDate";
    private static final Set<Operator> ALLOWED_OPERATORS = Set.of(
            Operator.EQ
    );

    private static final String DATE_PATTERN = "^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(Z|[+-]\\d{2}:\\d{2}))$";

    @Override
    public boolean evaluate(Operator operator, Object rightOperand, Permission rule, C context) {
        try {
            var expiryDate = Instant.parse(rightOperand.toString());
            return Instant.now().truncatedTo(ChronoUnit.SECONDS).isBefore(expiryDate);
        } catch (DateTimeParseException e) {
            context.reportProblem("Invalid right-operand: right operand must match pattern '%s'".formatted(DATE_PATTERN));
            return false;
        }
    }

    @Override
    public Result<Void> validate(Operator operator, Object rightOperand, Permission rule) {
        if (rightOperand == null) {
            return Result.failure("Invalid operator: this constraint only allows the following operators: %s, but received null.".formatted(ALLOWED_OPERATORS));
        }

        if (!ALLOWED_OPERATORS.contains(operator)) {
            return Result.failure("Invalid operator: this constraint only allows the following operators: %s, but received '%s'.".formatted(ALLOWED_OPERATORS, operator));
        }

        var compiledPattern = Pattern.compile(DATE_PATTERN);
        if (!(rightOperand instanceof String rightValue && compiledPattern.matcher(rightValue).matches())) {
            return Result.failure("Invalid right-operand: right operand must match pattern '%s'".formatted(DATE_PATTERN));
        }

        try {
            Instant.parse(rightValue);
        } catch (DateTimeParseException e) {
            return Result.failure("Invalid right-operand: right operand must be a valid ISO-8061 UTC date-time string matching pattern '%s'".formatted(DATE_PATTERN));
        }

        return Result.success();
    }
}
