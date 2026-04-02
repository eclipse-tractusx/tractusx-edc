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
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Rule;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

/**
 * This is an abstract constraint function for DataEndDurationDays constraints. It evaluates to true if the current
 * date is before the expiry date, which is calculated by adding the specified number of days to the contract signing
 * date. The contract signing date is retrieved from the agreement context.
 *
 * @param <R> the type of the rule (e.g. Permission, Duty, etc.)
 * @param <C> the type of the agreement policy context
 */
public abstract class AbstractDataEndDurationDaysConstraintFunction<R extends Rule, C extends AgreementPolicyContext> implements AtomicConstraintRuleFunction<R, C> {
    private static final Set<Operator> ALLOWED_OPERATORS = Set.of(
            Operator.EQ
    );
    private final Monitor monitor;

    protected AbstractDataEndDurationDaysConstraintFunction(Monitor monitor) {
        this.monitor = monitor.withPrefix(getClass().getSimpleName());
    }

    private Result<Integer> extractRightValue(Object rightOperand) {
        if (rightOperand instanceof Integer) {
            return Result.success((Integer) rightOperand);
        } else if (rightOperand instanceof String rightValue) {
            try {
                return Result.success(Integer.parseInt(rightValue));
            } catch (NumberFormatException e) {
                return Result.failure("Invalid right-operand: value must be a valid integer, but got '%s'.".formatted(rightOperand));
            }
        }

        return Result.failure("Invalid right-operand: this constraint only allows integer values or strings representing integers, but got '%s'."
                .formatted(rightOperand != null ? rightOperand.getClass().getName() : "null"));
    }

    @Override
    public boolean evaluate(Operator operator, Object rightOperand, R rule, C context) {
        return extractRightValue(rightOperand)
                .map(rightValue -> Instant.ofEpochSecond(context.contractAgreement().getContractSigningDate())
                        .truncatedTo(ChronoUnit.DAYS)
                        .plus(rightValue, ChronoUnit.DAYS))
                .map(expiryDate -> context.now().truncatedTo(ChronoUnit.DAYS).isBefore(expiryDate))
                .orElse(failure -> {
                    var msg = "Failed to evaluate constraint due to invalid right operand: '%s'. Problems: %s".formatted(rightOperand, failure);
                    monitor.debug(msg);
                    context.reportProblem(msg);
                    return false;
                });
    }

    @Override
    public Result<Void> validate(Operator operator, Object rightValue, R rule) {
        if (operator == null) {
            return Result.failure("Invalid operator: this constraint only allows the following operators: %s, but received null.".formatted(ALLOWED_OPERATORS));
        }

        if (!ALLOWED_OPERATORS.contains(operator)) {
            return Result.failure("Invalid operator: this constraint only allows the following operators: %s, but received '%s'.".formatted(ALLOWED_OPERATORS, operator));
        }

        return extractRightValue(rightValue).mapEmpty();
    }
}
