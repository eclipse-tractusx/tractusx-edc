package org.eclipse.tractusx.edc.iam.dcp.api;

import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;
import org.eclipse.tractusx.edc.iam.dcp.api.v3.VerifiablePresentationCacheApiController;
import org.eclipse.tractusx.edc.spi.dcp.VerifiablePresentationCache;

public class VerifiablePresentationCacheApiExtension implements ServiceExtension {

    @Inject
    private WebService webService;
    @Inject
    private VerifiablePresentationCache cache;
    @Inject
    private SingleParticipantContextSupplier participantContextSupplier;

    @Override
    public void initialize(ServiceExtensionContext context) {
        webService.registerResource(ApiContext.MANAGEMENT, new VerifiablePresentationCacheApiController(cache,  participantContextSupplier));
    }

}
