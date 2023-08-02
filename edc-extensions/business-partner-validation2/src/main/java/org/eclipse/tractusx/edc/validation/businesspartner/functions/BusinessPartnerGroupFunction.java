/*
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.validation.businesspartner.functions;

import org.eclipse.edc.policy.engine.spi.AtomicConstraintFunction;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.eclipse.tractusx.edc.validation.businesspartner.spi.BusinessPartnerGroupStore;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.eclipse.edc.policy.model.Operator.EQ;
import static org.eclipse.edc.policy.model.Operator.IN;
import static org.eclipse.edc.policy.model.Operator.IS_ALL_OF;
import static org.eclipse.edc.policy.model.Operator.IS_ANY_OF;
import static org.eclipse.edc.policy.model.Operator.IS_NONE_OF;
import static org.eclipse.edc.policy.model.Operator.NEQ;

/**
 * This function evaluates, that a particular {@link ParticipantAgent} is a member of a particular group.
 * The {@link ParticipantAgent} is represented by its BPN, the {@link org.eclipse.edc.policy.model.Operator} and the {@code rightValue} determine the group(s) and
 * whether the BPN must part of it, or not be part of it.
 * <p>
 * For example, a {@link org.eclipse.edc.policy.model.Policy} that mandates the BPN be part of a group {@code "gold_customers"} or {@code "platin_partner} could look like this:
 *
 * <pre>
 * {
 *     "constraint": {
 *         "leftOperand": "BusinessPartnerGroup",
 *         "operator": "isAnyOf",
 *         "rightOperand": ["gold_customer","platin_partner"]
 *     }
 * }
 * </pre>
 * <p>
 * Upon evaluation, the {@link BusinessPartnerGroupFunction} will take the {@link ParticipantAgent}s BPN, use it to resolve the groups that the BPN is part of, and check, whether `"gold_partner"` and
 * `"platin_partner"` are amongst those groups.
 * <p/>
 * The following operators are supported:
 * <ul>
 *     <li>{@link Operator#EQ}: must be exactly in - and only in - that particular group</li>
 *     <li>{@link Operator#NEQ}: must not be in a particular group</li>
 *     <li>{@link Operator#IN}: must be in <em>any</em> of the specified groups</li>
 *     <li>{@link Operator#IS_ALL_OF}: must be in <em>all</em> of the specified groups</li>
 *     <li>{@link Operator#IS_ANY_OF}: must be in <em>any</em> of the specified groups</li>
 *     <li>{@link Operator#IS_NONE_OF}: must <em>not</em> be in any of the specified groups</li>
 * </ul>
 *
 * @see BusinessPartnerGroupStore
 */

public class BusinessPartnerGroupFunction implements AtomicConstraintFunction<Permission> {
    public static final String REFERRING_CONNECTOR_CLAIM = "referringConnector";
    private static final List<Operator> ALLOWED_OPERATORS = List.of(EQ, NEQ, IN, IS_ALL_OF, IS_ANY_OF, IS_NONE_OF);
    private static final Map<Operator, Function<BpnGroupTuple, Boolean>> OPERATOR_EVALUATOR_MAP = new HashMap<>();
    private final BusinessPartnerGroupStore store;

    public BusinessPartnerGroupFunction(BusinessPartnerGroupStore store) {
        this.store = store;
        OPERATOR_EVALUATOR_MAP.put(EQ, this::evaluateEquals);
        OPERATOR_EVALUATOR_MAP.put(NEQ, this::evaluateNotEquals);
        OPERATOR_EVALUATOR_MAP.put(IN, this::evaluateIn);
        OPERATOR_EVALUATOR_MAP.put(IS_ALL_OF, this::evaluateEquals);
        OPERATOR_EVALUATOR_MAP.put(IS_ANY_OF, this::evaluateIn);
        OPERATOR_EVALUATOR_MAP.put(IS_NONE_OF, this::evaluateNotEquals);
    }


    @Override
    public boolean evaluate(Operator operator, Object rightValue, Permission rule, PolicyContext policyContext) {
        final ParticipantAgent participantAgent = policyContext.getContextData(ParticipantAgent.class);

        if (participantAgent == null) {
            policyContext.reportProblem("ParticipantAgent not found on PolicyContext");
            return false;
        }
        if (!ALLOWED_OPERATORS.contains(operator)) {
            var ops = ALLOWED_OPERATORS.stream().map(Enum::name).collect(Collectors.joining(", "));
            policyContext.reportProblem(format("Operator must be one of [%s] but was [%s]", ops, operator.name()));
            return false;
        }

        var bpn = getBpnClaim(participantAgent);
        var groups = store.resolveForBpn(bpn);
        if (groups.failed()) {
            policyContext.reportProblem(groups.getFailureDetail());
            return false;
        }

        var rightOperand = parseRightOperand(rightValue, policyContext);
        if (rightOperand == null) {
            return false;
        }

        return OPERATOR_EVALUATOR_MAP.get(operator).apply(new BpnGroupTuple(groups.getContent(), rightOperand));
    }

    private List<String> parseRightOperand(Object rightValue, PolicyContext context) {
        if (rightValue instanceof String) {
            return List.of(rightValue.toString());
        }
        if (rightValue instanceof Collection<?>) {
            return ((Collection<?>) rightValue).stream().map(Object::toString).toList();
        }

        context.reportProblem(format("Right operand expected to be either String or a Collection, but was " + rightValue.getClass()));
        return null;
    }

    private String getBpnClaim(ParticipantAgent participantAgent) {
        String bpnClaim = null;
        var claims = participantAgent.getClaims();

        var bpnClaimObject = claims.get(REFERRING_CONNECTOR_CLAIM);

        if (bpnClaimObject instanceof String) {
            bpnClaim = (String) bpnClaimObject;
        }
        if (bpnClaim == null) {
            bpnClaim = participantAgent.getIdentity();
        }
        return bpnClaim;
    }

    private Boolean evaluateIn(BpnGroupTuple bpnGroupTuple) {
        var assigned = bpnGroupTuple.assignedGroups;
        // checks whether both lists overlap
        return bpnGroupTuple.allowedGroups
                .stream()
                .distinct()
                .anyMatch(assigned::contains);
    }

    private Boolean evaluateNotEquals(BpnGroupTuple bpnGroupTuple) {
        return !evaluateIn(bpnGroupTuple);
    }

    private Boolean evaluateEquals(BpnGroupTuple bpnGroupTuple) {
        return bpnGroupTuple.allowedGroups.equals(bpnGroupTuple.assignedGroups);
    }

    private record BpnGroupTuple(List<String> assignedGroups, List<String> allowedGroups) {
    }
}
