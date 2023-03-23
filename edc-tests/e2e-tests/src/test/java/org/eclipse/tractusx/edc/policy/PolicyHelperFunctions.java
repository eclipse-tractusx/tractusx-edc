package org.eclipse.tractusx.edc.policy;


import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.OrConstraint;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PolicyHelperFunctions {
    /**
     * Creates a {@link PolicyDefinition} using the given ID, that contains equality constraints for each of the given BusinessPartnerNumbers:
     * each BPN is converted into an {@link AtomicConstraint} {@code BusinessPartnerNumber EQ [BPN]}.
     */
    public static PolicyDefinition businessPartnerNumberPolicy(String id, String... bpns) {

        var bpnConstraints = Stream.of(bpns)
                .map(bpn -> (Constraint) AtomicConstraint.Builder.newInstance()
                        .leftExpression(new LiteralExpression("BusinessPartnerNumber"))
                        .operator(Operator.EQ)
                        .rightExpression(new LiteralExpression(bpn))
                        .build())
                .collect(Collectors.toList());

        return PolicyDefinition.Builder.newInstance()
                .id(id)
                .policy(Policy.Builder.newInstance()
                        .permission(Permission.Builder.newInstance()
                                .action(Action.Builder.newInstance().type("USE").build())
                                .constraint(OrConstraint.Builder.newInstance()
                                        .constraints(bpnConstraints)
                                        .build())
                                .build()).build()).build();
    }
}
