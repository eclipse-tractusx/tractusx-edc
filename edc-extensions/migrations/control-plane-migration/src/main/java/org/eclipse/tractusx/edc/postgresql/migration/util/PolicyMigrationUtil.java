package org.eclipse.tractusx.edc.postgresql.migration.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.MultiplicityConstraint;
import org.eclipse.edc.policy.model.Rule;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PolicyMigrationUtil {

    private PolicyMigrationUtil() {
    }

    public static List<Rule> ruleDeserializer(ObjectMapper mapper, String ruleJson) throws JsonProcessingException {
        return mapper.readValue(ruleJson, new TypeReference<List<Rule>>() {
        });
    }

    public static <T> String ruleSerializer(ObjectMapper mapper, List<Rule> rules, TypeReference<T> typeReference) throws JsonProcessingException {
        return mapper.writerFor(typeReference).writeValueAsString(rules);
    }

    public static boolean rulesContainsLeftExpression(List<Rule> rules, Set<String> leftExpressions) {
        return numberOfConstraintsInRulesWithLeftExpression(rules, leftExpressions) > 0;
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
        for (int i = 0; i < constraints.size(); i++) {
            var constraint = constraints.get(i);
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

    public static void updateBusinessPartnerRules(List<Rule> rules, Map<String, String> namespaceMapping) {
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
}
