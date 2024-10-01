package org.eclipse.tractusx.edc.agreements.retirement.defaults;


import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.tractusx.edc.agreements.retirement.spi.store.AgreementsRetirementStore;

@Extension("Provides a default Agreements")
public class DefaultStoreProviderExtension implements ServiceExtension {

    @Inject
    CriterionOperatorRegistry criterionOperatorRegistry;

    @Provider(isDefault = true)
    public AgreementsRetirementStore createInMemStore(){
        return new InMemoryAgreementsRetirementStore(criterionOperatorRegistry);
    }

}
