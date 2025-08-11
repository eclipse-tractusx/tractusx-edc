package org.eclipse.tractusx.edc.postgresql.migration.util;

import org.eclipse.edc.connector.controlplane.contract.spi.ContractOfferId;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.entity.ProtocolMessages;

import java.time.Instant;
import java.util.UUID;

public class ContractNegotiationMigrationUtil {
    private ContractNegotiationMigrationUtil() {}

    public static ContractNegotiation createNegotiation(String id, Policy policy) {
        return createNegotiationBuilder(id)
                .contractAgreement(createContract(ContractOfferId.create("test-cd1", "test-as1"), policy))
                .build();
    }

    public static ContractAgreement createContract(ContractOfferId contractOfferId, Policy policy) {
        return createContractBuilder(contractOfferId.toString(), policy)
                .build();
    }
    private static ContractNegotiation.Builder createNegotiationBuilder(String id) {
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

    private static ContractAgreement.Builder createContractBuilder(String id, Policy policy) {
        return ContractAgreement.Builder.newInstance()
                .id(id)
                .providerId("provider")
                .consumerId("consumer")
                .assetId(UUID.randomUUID().toString())
                .policy(policy)
                .contractSigningDate(Instant.now().getEpochSecond());
    }
}
