package org.eclipse.tractusx.edc.postgresql.migration.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private PolicyMigrationUtil() {
    }

    public static Policy policyDeserializer(ObjectMapper mapper, String policyJson) throws JsonProcessingException {
        return mapper.readValue(policyJson, new TypeReference<Policy>() {
        });
    }

    public static List<Rule> ruleDeserializer(ObjectMapper mapper, String ruleJson) throws JsonProcessingException {
        return mapper.readValue(ruleJson, new TypeReference<List<Rule>>() {
        });
    }

    public static String policySerializer(ObjectMapper mapper, Policy policy) throws JsonProcessingException {
        return mapper.writeValueAsString(policy);
    }

    public static <T> String ruleSerializer(ObjectMapper mapper, List<? extends Rule> rules, TypeReference<T> typeReference) throws JsonProcessingException {
        return mapper.writerFor(typeReference).writeValueAsString(rules);
    }

    public static boolean rulesContainsLeftExpression(List<? extends Rule> rules, Set<String> leftExpressions) {
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
