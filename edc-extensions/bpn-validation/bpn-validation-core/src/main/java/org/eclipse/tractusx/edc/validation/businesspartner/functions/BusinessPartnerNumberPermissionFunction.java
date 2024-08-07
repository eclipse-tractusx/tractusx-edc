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

import org.eclipse.edc.policy.engine.spi.AtomicConstraintFunction;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.eclipse.edc.spi.result.Failure;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static org.eclipse.edc.policy.model.Operator.EQ;
import static org.eclipse.edc.policy.model.Operator.HAS_PART;
import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

/**
 * AtomicConstraintFunction to validate business partner numbers for edc permissions.
 */
public class BusinessPartnerNumberPermissionFunction implements AtomicConstraintFunction<Permission> {

    private static final List<Operator> SUPPORTED_OPERATORS = Arrays.asList(
            EQ,
            Operator.IN,
            Operator.NEQ,
            Operator.IS_ANY_OF,
            Operator.IS_A,
            Operator.IS_NONE_OF,
            Operator.IS_ALL_OF,
            Operator.HAS_PART
    );

    @Override
    public boolean evaluate(Operator operator, Object rightValue, Permission rule, PolicyContext context) {

        if (!SUPPORTED_OPERATORS.contains(operator)) {
            var message = "Operator %s is not supported. Supported operators: %s".formatted(operator, SUPPORTED_OPERATORS);
            context.reportProblem(message);
            return false;
        }

        var participantAgent = context.getContextData(ParticipantAgent.class);
        if (participantAgent == null) {
            context.reportProblem("Required PolicyContext data not found: " + ParticipantAgent.class.getName());
            return false;
        }

        var identity = participantAgent.getIdentity();
        if (identity == null) {
            context.reportProblem("Identity of the participant agent cannot be null");
            return false;
        }

        return switch (operator) {
            case EQ, IS_ALL_OF -> checkEquality(identity, rightValue, operator)
                    .orElse(reportFailure(context));
            case NEQ -> checkEquality(identity, rightValue, operator)
                    .map(b -> !b)
                    .orElse(reportFailure(context));
            case HAS_PART -> checkStringContains(identity, rightValue)
                    .orElse(reportFailure(context));
            case IN, IS_A, IS_ANY_OF ->
                    checkListContains(identity, rightValue, operator).orElse(reportFailure(context));
            case IS_NONE_OF -> checkListContains(identity, rightValue, operator)
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
        if (rightValue instanceof List numbers) {
            return success(numbers.contains(identity));
        }
        return failure("Invalid right-value: operator '%s' requires a 'List' but got a '%s'".formatted(operator, Optional.of(rightValue).map(Object::getClass).map(Class::getName).orElse(null)));
    }

    private Result<Boolean> checkStringContains(String identity, Object rightValue) {
        if (rightValue instanceof String bpnString) {
            return success(identity.contains(bpnString));
        }
        return failure("Invalid right-value: operator '%s' requires a 'String' but got a '%s'".formatted(HAS_PART, Optional.of(rightValue).map(Object::getClass).map(Class::getName).orElse(null)));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Result<Boolean> checkEquality(String identity, Object rightValue, Operator operator) {
        if (rightValue instanceof String bpnString) {
            return success(Objects.equals(identity, bpnString));
        } else if (rightValue instanceof List bpnList) {
            return success(bpnList.stream().allMatch(bpn -> Objects.equals(identity, bpn)));
        }
        return failure("Invalid right-value: operator '%s' requires a 'String' or a 'List' but got a '%s'".formatted(operator, Optional.of(rightValue).map(Object::getClass).map(Class::getName).orElse(null)));
    }
}
