/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.validation.businesspartner.functions;

import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.result.Failure;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

import static org.eclipse.edc.policy.model.Operator.EQ;
import static org.eclipse.edc.policy.model.Operator.HAS_PART;
import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

/**
 * AtomicConstraintFunction to validate business partner numbers for edc permissions.
 */
public class BusinessPartnerNumberPermissionFunction<C extends ParticipantAgentPolicyContext> implements AtomicConstraintRuleFunction<Permission, C> {

    private static final List<Operator> SUPPORTED_OPERATORS = Arrays.asList(
            Operator.IS_ANY_OF,
            Operator.IS_NONE_OF
    );

    @Override
    public boolean evaluate(Operator operator, Object rightOperand, Permission permission, ParticipantAgentPolicyContext context) {
        var participantAgent = context.participantAgent();

        if (!SUPPORTED_OPERATORS.contains(operator)) {
            var message = "Operator %s is not supported. Supported operators: %s".formatted(operator, SUPPORTED_OPERATORS);
            context.reportProblem(message);
            return false;
        }

        var identity = participantAgent.getIdentity();
        if (identity == null) {
            context.reportProblem("Identity of the participant agent cannot be null");
            return false;
        }

        return switch (operator) {
            case IS_ANY_OF ->
                    checkListContains(identity, rightOperand, operator).orElse(reportFailure(context));
            case IS_NONE_OF -> checkListContains(identity, rightOperand, operator)
                    .map(b -> !b)
                    .orElse(reportFailure(context));
            default -> false;
        };
    }

    private @NotNull Function<Failure, Boolean> reportFailure(PolicyContext context) {
        return f -> {
            context.reportProblem(f.getFailureDetail());
            return false;
        };
    }

    private Result<Boolean> checkListContains(String identity, Object rightValue, Operator operator) {
        if (rightValue instanceof List<?> numbers) {
            return success(numbers.contains(identity));
        }
        return failure("Invalid right-value: operator '%s' requires a 'List' but got a '%s'"
                .formatted(operator, Optional.of(rightValue).map(Object::getClass).map(Class::getName).orElse(null)));
    }

    @Override
    public Result<Void> validate(Operator operator, Object rightValue, Permission rule) {
        if (!SUPPORTED_OPERATORS.contains(operator)) {
            return Result.failure("Invalid operator: this constraint only allows the following operators: %s, but received '%s'."
                    .formatted(SUPPORTED_OPERATORS, operator));
        }

        var pattern = "^BPNL[0-9A-Z]{12}$";
        var compiledPattern = Pattern.compile(pattern);
        return rightValue instanceof String s && compiledPattern.matcher(s).matches()
                ? Result.success()
                : Result.failure("Invalid right-operand: right operand must match pattern '%s'".formatted(pattern));
    }
}
