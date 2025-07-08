/*
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.policy.cx.warrenty;

import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.result.Result;

import java.util.Set;

/**
 * This is a placeholder constraint function for ContractReference. It always returns true but allows
 * the validation of policies to be strictly enforced.
 */
public class WarrantyDurationMonthsConstraintFunction<C extends ParticipantAgentPolicyContext> implements AtomicConstraintRuleFunction<Permission, C> {
    public static final String WARRANTY_DURATION_MONTHS = "WarrantyDurationMonths";
    private static final Set<Operator> ALLOWED_OPERATORS = Set.of(
            Operator.EQ
    );

    @Override
    public boolean evaluate(Operator operator, Object rightOperand, Permission permission, C c) {
        return true;
    }

    @Override
    public Result<Void> validate(Operator operator, Object rightValue, Permission rule) {
        if (!ALLOWED_OPERATORS.contains(operator)) {
            return Result.failure("Invalid operator: this constraint only allows the following operators: %s, but received '%s'.".formatted(ALLOWED_OPERATORS, operator));
        }

        if (rightValue instanceof Integer) {
            return Result.success();
        }

        if (rightValue instanceof String stringValue) {
            try {
                Integer.parseInt(stringValue);
                return Result.success();
            } catch (NumberFormatException e) {
                return Result.failure("Invalid right-operand: String value must be a valid integer, but got '%s'.".formatted(stringValue));
            }
        }

        return Result.failure("Invalid right-operand: this constraint only allows integer values or strings representing integers, but got '%s'."
                .formatted(rightValue != null ? rightValue.getClass().getName() : "null"));
    }
}
