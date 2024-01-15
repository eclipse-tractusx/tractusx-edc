/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.iatp.policy;

import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.agent.ParticipantAgent;

import java.time.Instant;
import java.util.Map;
import java.util.function.BiFunction;

public class MembershipCredentialEvaluationFunction extends BaseCredentialEvaluationFunction {

    public static final String START_TIME = "startTime";
    public static final String MEMBER_OF = "memberOf";
    public static final String STATUS = "status";
    public static final String ACTIVE = "Active";
    public static final String CATENA_X = "Catena-X";
    private final Map<String, BiFunction<PolicyContext, String, Boolean>> claimsCheckers = Map.of(
            STATUS, this::validateStatus,
            MEMBER_OF, this::validateMemberOf,
            START_TIME, this::validateStartTime
    );

    @Override
    public boolean evaluate(Operator operator, Object rightOperand, Permission permission, PolicyContext policyContext) {

        if (!operator.equals(Operator.EQ)) {
            policyContext.reportProblem("Invalid operator '%s', only accepts '%s'".formatted(operator, Operator.EQ));
            return false;
        }
        var pa = policyContext.getContextData(ParticipantAgent.class);
        if (pa == null) {
            policyContext.reportProblem("No ParticipantAgent found on context.");
            return false;
        }
        var claims = pa.getClaims();
        if ("active".equalsIgnoreCase(rightOperand.toString())) {
            return claimsCheckers.entrySet().stream()
                    .reduce(true, (i, checker) -> checker.getValue().apply(policyContext, getClaim(String.class, checker.getKey(), claims)), (first, left) -> first && left);
        }
        return false;
    }

    private boolean validateMemberOf(PolicyContext policyContext, String memberOf) {
        return validateField(policyContext, MEMBER_OF, CATENA_X, memberOf);

    }

    private boolean validateStatus(PolicyContext policyContext, String status) {
        return validateField(policyContext, STATUS, ACTIVE, status);
    }

    private boolean validateField(PolicyContext policyContext, String field, String expectedValue, String currentValue) {
        if (expectedValue.equals(currentValue)) {
            return true;
        } else {
            policyContext.reportProblem("Invalid membership %s '%s', only accepts '%s'".formatted(field, currentValue, expectedValue));
            return false;
        }
    }

    private boolean validateStartTime(PolicyContext policyContext, String since) {
        var membershipStartDate = Instant.parse(since);
        if (membershipStartDate.isBefore(Instant.now())) {
            return true;
        } else {
            policyContext.reportProblem("Invalid membership start date");
            return false;
        }
    }
}
