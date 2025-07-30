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

import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.monitor.Monitor;
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
import static org.eclipse.edc.policy.model.Operator.EQ;
import static org.eclipse.edc.policy.model.Operator.IN;
import static org.eclipse.edc.policy.model.Operator.IS_ALL_OF;
import static org.eclipse.edc.policy.model.Operator.IS_ANY_OF;
import static org.eclipse.edc.policy.model.Operator.IS_NONE_OF;
import static org.eclipse.edc.policy.model.Operator.NEQ;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_NAMESPACE;

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
 *         "leftOperand": "https://w3id.org/tractusx/v0.0.1/ns/BusinessPartnerGroup",
 *         "operator": "isAnyOf",
 *         "rightOperand": ["gold_customer","platin_partner"]
 *     }
 * }
 * </pre>
 * <p>
 * Upon evaluation, the {@link BusinessPartnerGroupFunction} will take the {@link ParticipantAgent}s BPN, use it to resolve the groups that the BPN is part of, and check, whether `"gold_partner"` and
 * `"platin_partner"` are amongst those groups.
 * <p>
 * The following operators are supported:
 * <ul>
 *     <li>{@link Operator#EQ}: must be exactly in - and only in - that particular group or set of groups</li>
 *     <li>{@link Operator#NEQ}: must not be in a particular group or set of groups</li>
 *     <li>{@link Operator#IN}: must be in <em>any</em> of the specified groups</li>
 *     <li>{@link Operator#IS_ALL_OF}: must be in <em>all</em> of the specified groups</li>
 *     <li>{@link Operator#IS_ANY_OF}: must be in <em>any</em> of the specified groups</li>
 *     <li>{@link Operator#IS_NONE_OF}: must <em>not</em> be in any of the specified groups</li>
 * </ul>
 *
 * @see BusinessPartnerStore
 */
public class BusinessPartnerGroupFunction<C extends ParticipantAgentPolicyContext> implements AtomicConstraintRuleFunction<Permission, C> {
    public static final String BUSINESS_PARTNER_CONSTRAINT_KEY = TX_NAMESPACE + "BusinessPartnerGroup";
    private static final List<Operator> ALLOWED_OPERATORS = List.of(EQ, NEQ, IN, IS_ALL_OF, IS_ANY_OF, IS_NONE_OF);
    private static final Map<Operator, Function<BpnGroupHolder, Boolean>> OPERATOR_EVALUATOR_MAP = new HashMap<>();
    private final BusinessPartnerStore store;
    private final Monitor monitor;

    public BusinessPartnerGroupFunction(BusinessPartnerStore store, Monitor monitor) {
        this.store = store;
        this.monitor = monitor;
        OPERATOR_EVALUATOR_MAP.put(EQ, this::evaluateEquals);
        OPERATOR_EVALUATOR_MAP.put(NEQ, this::evaluateNotEquals);
        OPERATOR_EVALUATOR_MAP.put(IN, this::evaluateIsAnyOf);
        OPERATOR_EVALUATOR_MAP.put(IS_ALL_OF, this::evaluateIsAllOf);
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

        var bpn = participantAgent.getIdentity();
        var groups = store.resolveForBpn(bpn);

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

    @Deprecated(since = "0.9.0")
    private Boolean evaluateNotEquals(BpnGroupHolder bpnGroupHolder) {
        monitor.warning("%s is a deprecated operator, in future please use %s operator.".formatted(NEQ, IS_NONE_OF));
        return !bpnGroupHolder.allowedGroups.equals(bpnGroupHolder.assignedGroups);
    }

    @Deprecated(since = "0.9.0")
    private Boolean evaluateEquals(BpnGroupHolder bpnGroupHolder) {
        monitor.warning("%s is a deprecated operator, in future please use %s operator.".formatted(EQ, IS_ALL_OF));
        return bpnGroupHolder.allowedGroups.equals(bpnGroupHolder.assignedGroups);
    }

    private Boolean evaluateIsAllOf(BpnGroupHolder bpnGroupHolder) {
        var assigned = bpnGroupHolder.assignedGroups;
        var allowed = bpnGroupHolder.allowedGroups;
        return (assigned.isEmpty() || !allowed.isEmpty()) && assigned.containsAll(allowed);
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
