package org.eclipse.tractusx.edc.iam.dcp;

import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.tractusx.edc.iam.dcp.cache.store.InMemoryVerifiablePresentationCacheStore;
import org.eclipse.tractusx.edc.spi.dcp.VerifiablePresentationCacheStore;

public class VerifiablePresentationCacheDefaultExtension implements ServiceExtension {

    @Provider(isDefault = true)
    public VerifiablePresentationCacheStore inMemoryVerifiablePresentationCacheStore() {
        return new InMemoryVerifiablePresentationCacheStore();
    }
}
