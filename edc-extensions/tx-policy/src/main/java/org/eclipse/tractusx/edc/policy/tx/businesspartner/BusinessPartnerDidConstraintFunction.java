/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * Copyright (c) 2026 Catena-X Automotive Network e.V.
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

package org.eclipse.tractusx.edc.policy.tx.businesspartner;

import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.result.Failure;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.tractusx.edc.policy.cx.common.ValueValidatingConstraintFunction;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

/**
 * Constraint function that evaluates the Decentralized Identifier (DID) of a participant agent.
 * <p>
 * Supports access policies where the left-operand is {@code "BusinessPartnerDID"} under
 * namespace {@code https://w3id.org/tractusx/policy/2.0.0/}.
 * <p>
 * In DCP/IATP flows the participant agent's identity is already a DID, so no BDRS
 * resolution is required — the identity is compared directly against the right-operand
 * DID strings.
 */
public class BusinessPartnerDidConstraintFunction<C extends ParticipantAgentPolicyContext>
        extends ValueValidatingConstraintFunction<Permission, C> {

    public static final String BUSINESS_PARTNER_DID = "BusinessPartnerDID";

    private static final List<Operator> SUPPORTED_OPERATORS = Arrays.asList(
            Operator.IS_ANY_OF,
            Operator.IS_NONE_OF
    );

    public BusinessPartnerDidConstraintFunction() {
        super(
                Set.of(Operator.IS_ANY_OF, Operator.IS_NONE_OF),
                "^did:.+$",
                true
        );
    }

    @Override
    public boolean evaluate(Operator operator, Object rightOperand, Permission permission,
                            ParticipantAgentPolicyContext context) {

        if (!SUPPORTED_OPERATORS.contains(operator)) {
            context.reportProblem("Operator %s is not supported. Supported operators: %s"
                    .formatted(operator, SUPPORTED_OPERATORS));
            return false;
        }

        var identity = context.participantAgent().getIdentity();
        if (identity == null) {
            context.reportProblem("Identity of the participant agent cannot be null");
            return false;
        }

        return switch (operator) {
            case IS_ANY_OF -> checkListContains(identity, rightOperand, operator)
                    .orElse(reportFailure(context));
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
        if (rightValue instanceof List<?> dids) {
            boolean found = dids.stream()
                    .filter(entry -> entry instanceof Map<?, ?>)
                    .map(entry -> ((Map<?, ?>) entry).get("@value"))
                    .filter(value -> value instanceof Map<?, ?>)
                    .map(value -> ((Map<?, ?>) value).get("string"))
                    .anyMatch(identity::equals);
            return success(found);
        } else if (rightValue instanceof String singleDid) {
            return success(identity.equals(singleDid));
        }
        return failure("Invalid right-value: operator '%s' requires a List or String but got '%s'"
                .formatted(operator,
                        Optional.ofNullable(rightValue).map(o -> o.getClass().getName()).orElse("null")));
    }
}

