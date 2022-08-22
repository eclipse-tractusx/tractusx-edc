package net.catenax.edc.cp.adapter.process.contractdatastore;

import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;

import java.util.HashMap;
import java.util.Map;

public class InMemoryContractDataStore implements ContractDataStore{
    private final static Map<String, ContractAgreementData> contractMap = new HashMap<>();

    @Override
    public void add(String assetId, String provider, String negotiationId, ContractAgreement agreement) {
        contractMap.put(getKey(assetId, provider), ContractAgreementData.from(agreement, negotiationId));
    }

    @Override
    public ContractAgreementData get(String assetId, String provider) {
        return contractMap.get(getKey(assetId, provider));
    }

    private String getKey(String assetId, String provider){
        return assetId + "::" + provider;
    }
}
