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

package org.eclipse.tractusx.edc.policy.cx.businesspartner;

import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.result.Failure;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.tractusx.edc.policy.cx.common.ValueValidatingConstraintFunction;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;
import static org.eclipse.tractusx.edc.spi.identity.mapper.BdrsConstants.DID_PREFIX;

/**
 * This is a constraint function that evaluates the BusinessPartnerNumber of a participant agent.
 */
public class BusinessPartnerNumberConstraintFunction<C extends ParticipantAgentPolicyContext> extends ValueValidatingConstraintFunction<Permission, C> {
    public static final String BUSINESS_PARTNER_NUMBER = "BusinessPartnerNumber";

    private static final List<Operator> SUPPORTED_OPERATORS = Arrays.asList(
            Operator.IS_ANY_OF,
            Operator.IS_NONE_OF
    );

    private BdrsClient bdrsClient;

    public BusinessPartnerNumberConstraintFunction(BdrsClient bdrsClient) {
        super(
                Set.of(Operator.IS_ANY_OF, Operator.IS_NONE_OF),
                "^BPNL[0-9A-Z]{12}$",
                true
        );

        this.bdrsClient = bdrsClient;
    }

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

        if (identity.startsWith(DID_PREFIX)) {
            identity = bdrsClient.resolveBpn(identity);
        }

        return switch (operator) {
            case IS_ANY_OF -> checkListContains(identity, rightOperand, operator).orElse(reportFailure(context));
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
            boolean containsBpn = numbers.stream()
                    .filter(entry -> entry instanceof Map<?, ?>)
                    .map(entry -> ((Map<?, ?>) entry).get("@value"))
                    .filter(value -> value instanceof Map<?, ?>)
                    .map(value -> ((Map<?, ?>) value).get("string"))
                    .anyMatch(bpn -> identity.equals(bpn));

            return success(containsBpn);
        } else if (rightValue instanceof String singleNumber) {
            boolean containsBpn = identity.equals(singleNumber);
            return success(containsBpn);
        }
        return failure("Invalid right-value: operator '%s' requires a 'List' but got a '%s'"
                .formatted(operator, Optional.of(rightValue).map(Object::getClass).map(Class::getName).orElse(null)));
    }
}
