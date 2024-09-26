package org.eclipse.tractusx.edc.agreements.retirement;

import org.eclipse.tractusx.edc.agreements.retirement.defaults.InMemoryAgreementsRetirementStore;
import org.eclipse.tractusx.edc.agreements.retirement.spi.AgreementsRetirementStore;
import org.eclipse.tractusx.edc.agreements.retirement.store.AgreementsRetirementStoreTestBase;

import static org.junit.jupiter.api.Assertions.*;

public class InMemoryAgreementsRetirementStoreTest extends AgreementsRetirementStoreTestBase {

    private final InMemoryAgreementsRetirementStore store = new InMemoryAgreementsRetirementStore();

    @Override
    protected AgreementsRetirementStore getStore() {
        return store;
    }
}