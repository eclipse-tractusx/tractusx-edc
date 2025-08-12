package org.eclipse.tractusx.edc.postgresql.migration.util;

import org.eclipse.edc.connector.controlplane.contract.spi.ContractOfferId;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.entity.ProtocolMessages;

import java.time.Instant;
import java.util.Arrays;

public class ContractNegotiationMigrationUtil {
    private ContractNegotiationMigrationUtil() {
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
