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
import org.eclipse.tractusx.edc.policy.cx.common.ValueValidatingConstraintFunction;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;
import org.eclipse.tractusx.edc.validation.businesspartner.spi.store.BusinessPartnerStore;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.eclipse.edc.policy.model.Operator.IS_ANY_OF;
import static org.eclipse.edc.policy.model.Operator.IS_NONE_OF;
import static org.eclipse.tractusx.edc.spi.identity.mapper.BdrsConstants.DID_PREFIX;

/**
 * This is a placeholder constraint function for BusinessPartnerGroup. It always returns true but allows
 * the validation of policies to be strictly enforced.
 */
public class BusinessPartnerGroupConstraintFunction<C extends ParticipantAgentPolicyContext> extends ValueValidatingConstraintFunction<Permission, C> {
    public static final String BUSINESS_PARTNER_GROUP = "BusinessPartnerGroup";

    private static final List<Operator> ALLOWED_OPERATORS = List.of(IS_ANY_OF, IS_NONE_OF);
    private static final Map<Operator, Function<BpnGroupHolder, Boolean>> OPERATOR_EVALUATOR_MAP = new HashMap<>();
    private final BusinessPartnerStore store;
    private BdrsClient bdrsClient;

    public BusinessPartnerGroupConstraintFunction(BusinessPartnerStore store, BdrsClient bdrsClient) {
        super(
                Set.of(Operator.IS_ANY_OF, Operator.IS_NONE_OF),
                "[\\s\\S]+",
                true
        );
        this.store = store;
        this.bdrsClient = bdrsClient;
        OPERATOR_EVALUATOR_MAP.put(IS_ANY_OF, this::evaluateIsAnyOf);
        OPERATOR_EVALUATOR_MAP.put(IS_NONE_OF, this::evaluateIsNoneOf);
    }

    @Override
    public boolean evaluate(Operator operator, Object rightOperand, Permission permission, C context) {
        var participantAgent = context.participantAgent();
        // invalid operator
        if (!ALLOWED_OPERATORS.contains(operator)) {
            var ops = ALLOWED_OPERATORS.stream().map(Enum::name).collect(Collectors.joining(", "));
            context.reportProblem(format("Operator must be one of [%s] but was [%s]", ops, operator.name()));
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
        var groups = store.resolveForBpn(identity);

        var assignedGroups = groups.getContent();

        // BPN not found in database
        if (groups.failed()) {
            context.reportProblem(groups.getFailureDetail());
            return false;
        }

        // right-operand is anything other than String or Collection
        var parsedRightOperand = parseRightOperand(rightOperand, context);
        if (parsedRightOperand == null) {
            return false;
        }

        //call evaluator function
        return OPERATOR_EVALUATOR_MAP.get(operator).apply(new BpnGroupHolder(new HashSet<>(assignedGroups), parsedRightOperand));
    }

    private Set<String> parseRightOperand(Object rightValue, PolicyContext context) {
        if (rightValue instanceof String value) {
            var tokens = value.split(",");
            return Set.of(tokens);
        }
        if (rightValue instanceof Collection<?>) {
            return ((Collection<?>) rightValue).stream().map(Object::toString).collect(Collectors.toSet());
        }

        context.reportProblem(format("Right operand expected to be either String or a Collection, but was %s", rightValue.getClass()));
        return null;
    }

    private boolean evaluateIsAnyOf(BpnGroupHolder bpnGroupHolder) {
        if (bpnGroupHolder.allowedGroups.isEmpty() && bpnGroupHolder.assignedGroups.isEmpty()) {
            return true;
        }

        var allowedGroups = bpnGroupHolder.allowedGroups;
        return bpnGroupHolder.assignedGroups
                .stream()
                .anyMatch(allowedGroups::contains);
    }

    private boolean evaluateIsNoneOf(BpnGroupHolder bpnGroupHolder) {
        return !evaluateIsAnyOf(bpnGroupHolder);
    }

    /**
     * Internal utility class to hold the collection of assigned groups for a BPN, and the collection of groups specified in the policy ("allowed groups").
     */
    private record BpnGroupHolder(Set<String> assignedGroups, Set<String> allowedGroups) {
    }
}
