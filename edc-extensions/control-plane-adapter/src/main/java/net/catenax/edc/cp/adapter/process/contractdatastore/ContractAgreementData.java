package net.catenax.edc.cp.adapter.process.contractdatastore;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;

@Getter
@Setter
public class ContractAgreementData {
    private String id;
    private String providerAgentId;
    private String consumerAgentId;
    private long contractSigningDate;
    private long contractStartDate;
    private long contractEndDate;
    private String assetId;
    private String policyId;

    public static ContractAgreementData from(ContractAgreement agreement){
        ContractAgreementData data = new ContractAgreementData();
        data.setId(agreement.getId());
        data.setAssetId(agreement.getAssetId());
        data.setPolicyId(agreement.getPolicyId());
        data.setContractStartDate(agreement.getContractStartDate());
        data.setContractEndDate(agreement.getContractEndDate());
        data.setContractSigningDate(agreement.getContractSigningDate());
        return data;
    }
}
