package org.eclipse.tractusx.edc.agreements.retirement.spi.store;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.tractusx.edc.agreements.retirement.spi.types.AgreementsRetirementEntry;

import java.util.List;

@ExtensionPoint
public interface AgreementsRetirementStore  {
    String NOT_FOUND_TEMPLATE = "Contract Agreement with %s was not found on retirement list.";
    String ALREADY_EXISTS_TEMPLATE = "Contract Agreement %s is already retired.";

    StoreResult<Void> save(AgreementsRetirementEntry entry);

    StoreResult<Void> delete(String contractAgreementId);

    StoreResult<List<AgreementsRetirementEntry>> findRetiredAgreements(QuerySpec querySpec);

}
