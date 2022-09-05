package net.catenax.edc.cp.adapter.process.contractnotification;

import lombok.Getter;

public class ContractInfo {
    @Getter
    private String contractAgreementId;
    private ContractState contractState;

    public ContractInfo(String contractAgreementId, ContractState contractState) {
        this.contractAgreementId = contractAgreementId;
        this.contractState = contractState;
    }

    public ContractInfo(ContractState contractState) {
        this.contractState = contractState;
    }

    public boolean isConfirmed() {
        return ContractState.CONFIRMED.equals(contractState);
    }
    public boolean isDeclined() {
        return ContractState.DECLINED.equals(contractState);
    }
    public boolean isError() {
        return ContractState.ERROR.equals(contractState);
    }

    protected enum ContractState {
        CONFIRMED, DECLINED, ERROR;
    }
}