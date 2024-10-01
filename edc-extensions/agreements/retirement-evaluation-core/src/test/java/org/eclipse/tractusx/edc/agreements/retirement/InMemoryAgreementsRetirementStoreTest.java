package org.eclipse.tractusx.edc.agreements.retirement;

import org.eclipse.edc.query.CriterionOperatorRegistryImpl;
import org.eclipse.tractusx.edc.agreements.retirement.defaults.InMemoryAgreementsRetirementStore;
import org.eclipse.tractusx.edc.agreements.retirement.spi.store.AgreementsRetirementStore;
import org.eclipse.tractusx.edc.agreements.retirement.store.AgreementsRetirementStoreTestBase;


public class InMemoryAgreementsRetirementStoreTest extends AgreementsRetirementStoreTestBase {

    private final InMemoryAgreementsRetirementStore store = new InMemoryAgreementsRetirementStore(CriterionOperatorRegistryImpl.ofDefaults());

    @Override
    protected AgreementsRetirementStore getStore() {
        return store;
    }
}