package org.eclipse.tractusx.edc.postgresql.migration;


import org.eclipse.edc.connector.controlplane.contract.spi.ContractOfferId;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.AndConstraint;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.spi.entity.ProtocolMessages;

import java.time.Instant;
import java.util.Arrays;
import java.util.Set;

import static org.eclipse.tractusx.edc.postgresql.migration.util.PolicyMigrationUtil.numberOfConstraintsInRulesWithLeftExpression;

public final class PolicyMigrationTestFixture {
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

    public static ContractNegotiation negotiation(String id, Policy policy) {
        ContractOfferId contractOfferId = ContractOfferId.create("test-co1", policy.getTarget());
        return negotiationBuilder(id)
                .contractAgreement(contractAgreement(contractOfferId.toString(), policy))
                .contractOffers(Arrays.asList(contractOffer(contractOfferId.toString(), policy)))
                .build();
    }

    private static ContractNegotiation.Builder negotiationBuilder(String id) {
        return ContractNegotiation.Builder.newInstance()
                .type(ContractNegotiation.Type.CONSUMER)
                .id(id)
                .contractAgreement(null)
                .correlationId("corr-" + id)
                .state(ContractNegotiationStates.REQUESTED.code())
                .counterPartyAddress("consumer")
                .counterPartyId("consumerId")
                .protocol("protocol")
                .protocolMessages(new ProtocolMessages());
    }

    public static ContractAgreement contractAgreement(String id, Policy policy) {
        return ContractAgreement.Builder.newInstance()
                .id(id)
                .providerId("provider")
                .consumerId("consumer")
                .assetId(policy.getTarget())
                .policy(policy)
                .contractSigningDate(Instant.now().getEpochSecond())
                .build();
    }

    public static ContractOffer contractOffer(String id, Policy policy) {
        return ContractOffer.Builder.newInstance()
                .id(id)
                .policy(policy)
                .assetId(policy.getTarget())
                .build();
    }
}
