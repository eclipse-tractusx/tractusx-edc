/********************************************************************************
 * Copyright (c) 2025 Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
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

package org.eclipse.tractusx.edc.postgresql.migration.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.AndConstraint;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.MultiplicityConstraint;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.policy.model.Rule;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PolicyMigrationUtil {
    private static final Set<String> OLD_BPN_LEFT_EXPRESSIONS = Set.of(
            "https://w3id.org/tractusx/v0.0.1/ns/BusinessPartnerGroup",
            "https://w3id.org/tractusx/v0.0.1/ns/BusinessPartnerNumber"
    );

    private static final Map<String, String> UPDATED_BPN_LEFT_EXPRESSIONS = Map.of(
            "https://w3id.org/tractusx/v0.0.1/ns/BusinessPartnerGroup", "https://w3id.org/catenax/2025/9/policy/BusinessPartnerGroup",
            "https://w3id.org/tractusx/v0.0.1/ns/BusinessPartnerNumber", "https://w3id.org/catenax/2025/9/policy/BusinessPartnerNumber"
    );

    private PolicyMigrationUtil() {
    }

    public static boolean rulesContainsLeftExpression(List<? extends Rule> rules, Set<String> leftExpressions) {
        return numberOfConstraintsInRulesWithLeftExpression(rules, leftExpressions) > 0;
    }

    public static boolean updatePolicy(Policy policy) throws JsonProcessingException {
        return updateRules(policy.getPermissions(), policy.getProhibitions(), policy.getObligations());
    }

    public static boolean updateRules(List<? extends Rule> permissions, List<? extends Rule> prohibitions, List<? extends Rule> duties) throws JsonProcessingException {
        boolean permissionsUpdated = updateRules(permissions);
        boolean prohibitionsUpdated = updateRules(prohibitions);
        boolean dutiesUpdated = updateRules(duties);

        return permissionsUpdated || prohibitionsUpdated || dutiesUpdated;
    }

    private static boolean updateRules(List<? extends Rule> rules) {
        if (rulesContainsLeftExpression(rules, OLD_BPN_LEFT_EXPRESSIONS)) {
            updateBusinessPartnerRules(rules, UPDATED_BPN_LEFT_EXPRESSIONS);
            return true;
        }
        return false;
    }

    public static int numberOfConstraintsInRulesWithLeftExpression(List<? extends Rule> rules, Set<String> leftExpressions) {
        int rulesWithLeftExpression = 0;
        for (Rule rule : rules) {
            List<Constraint> constraints = rule.getConstraints();
            rulesWithLeftExpression += numberOfConstraintsWithLeftExpression(constraints, leftExpressions);
        }
        return rulesWithLeftExpression;
    }

    private static int numberOfConstraintsWithLeftExpression(List<Constraint> constraints, Set<String> leftExpressions) {
        int constraintsWithLeftExpression = 0;
        for (Constraint constraint : constraints) {
            if (constraint instanceof AtomicConstraint) {
                if (containsBusinessPartnerOperand((AtomicConstraint) constraint, leftExpressions)) {
                    constraintsWithLeftExpression++;
                }
            } else if (constraint instanceof MultiplicityConstraint) {
                constraintsWithLeftExpression += numberOfConstraintsWithLeftExpression(((MultiplicityConstraint) constraint).getConstraints(), leftExpressions);
            }
        }
        return constraintsWithLeftExpression;
    }

    private static boolean containsBusinessPartnerOperand(AtomicConstraint atomicConstraint, Set<String> leftExpressions) {
        var leftExpressionValue = getLeftExpressionValue(atomicConstraint);
        return leftExpressions.contains(leftExpressionValue);
    }

    private static Object getLeftExpressionValue(AtomicConstraint atomicConstraint) {
        if (atomicConstraint.getLeftExpression() instanceof LiteralExpression literalExpression) {
            return literalExpression.getValue();
        }
        return null;
    }

    public static void updateBusinessPartnerRules(List<? extends Rule> rules, Map<String, String> namespaceMapping) {
        for (Rule rule : rules) {
            List<Constraint> constraints = rule.getConstraints();
            updateBusinessPartnerConstraints(constraints, namespaceMapping);
        }
    }

    private static void updateBusinessPartnerConstraints(List<Constraint> constraints, Map<String, String> namespaceMapping) {
        for (int i = 0; i < constraints.size(); i++) {
            var constraint = constraints.get(i);
            if (constraint instanceof AtomicConstraint) {
                if (containsBusinessPartnerOperand((AtomicConstraint) constraint, namespaceMapping.keySet())) {
                    Constraint updatedConstraint = updateBpnLeftOperand(constraint, namespaceMapping);
                    constraints.set(i, updatedConstraint);
                }
            } else if (constraint instanceof MultiplicityConstraint) {
                updateBusinessPartnerConstraints(((MultiplicityConstraint) constraint).getConstraints(), namespaceMapping);
            }
        }
    }

    private static Constraint updateBpnLeftOperand(Constraint constraint, Map<String, String> namespaceMapping) {
        AtomicConstraint atomicConstraint = (AtomicConstraint) constraint;
        var leftExpressionValue = getLeftExpressionValue(atomicConstraint);

        return AtomicConstraint.Builder.newInstance()
                .leftExpression(new LiteralExpression(namespaceMapping.get(leftExpressionValue)))
                .operator(atomicConstraint.getOperator())
                .rightExpression(atomicConstraint.getRightExpression())
                .build();
    }

    public static int constraintsWithLeftExpressions(Policy policy, Set<String> leftExpressions) {
        return numberOfConstraintsInRulesWithLeftExpression(policy.getPermissions(), leftExpressions) +
                numberOfConstraintsInRulesWithLeftExpression(policy.getProhibitions(), leftExpressions) +
                numberOfConstraintsInRulesWithLeftExpression(policy.getObligations(), leftExpressions);
    }

    public static Constraint andConstraint(Constraint... constraints) {
        return AndConstraint.Builder.newInstance()
                .constraints(Arrays.asList(constraints))
                .build();
    }

    public static Constraint atomicConstraint(String leftExpression) {
        return AtomicConstraint.Builder.newInstance()
                .leftExpression(new LiteralExpression(leftExpression))
                .rightExpression(new LiteralExpression("right-value"))
                .operator(Operator.EQ)
                .build();
    }

    public static Permission permission(Constraint... constraints) {
        return Permission.Builder.newInstance()
                .action(Action.Builder.newInstance().type("use").build())
                .constraints(Arrays.asList(constraints))
                .build();
    }

    public static Prohibition prohibition(Constraint... constraints) {
        return Prohibition.Builder.newInstance()
                .action(Action.Builder.newInstance().type("use").build())
                .constraints(Arrays.asList(constraints))
                .build();
    }
}
