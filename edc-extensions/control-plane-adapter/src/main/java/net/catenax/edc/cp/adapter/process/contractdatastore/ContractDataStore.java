package net.catenax.edc.cp.adapter.process.contractdatastore;

import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;

public interface ContractDataStore {
    void add(String assetId, String provider, ContractAgreement contractAgreement);
    ContractAgreementData get(String assetId, String provider);
}
