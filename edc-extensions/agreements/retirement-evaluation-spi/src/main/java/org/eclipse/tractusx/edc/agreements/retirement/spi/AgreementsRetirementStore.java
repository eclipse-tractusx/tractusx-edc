package org.eclipse.tractusx.edc.agreements.retirement.spi;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.result.StoreResult;

@ExtensionPoint
public interface AgreementsRetirementStore  {
    String NOT_FOUND_TEMPLATE = "Contract Agreement with %s was not found on retirement list.";
    String ALREADY_EXISTS_TEMPLATE = "ContractAgreement %s is already retired.";

    StoreResult<Void> save(String contractAgreementId, String timestamp);

    StoreResult<Void> delete(String contractAgreementId);

    StoreResult<String> findRetiredAgreement(String contractAgreementId);

}
