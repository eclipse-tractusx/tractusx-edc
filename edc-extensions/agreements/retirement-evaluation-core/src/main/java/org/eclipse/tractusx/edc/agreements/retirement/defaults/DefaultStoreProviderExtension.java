package org.eclipse.tractusx.edc.agreements.retirement.defaults;


import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.tractusx.edc.agreements.retirement.spi.AgreementsRetirementStore;

@Extension("Provides a default Agreements")
public class DefaultStoreProviderExtension implements ServiceExtension {

    @Provider(isDefault = true)
    public AgreementsRetirementStore createInMemStore(){
        return new InMemoryAgreementsRetirementStore();
    }

}
