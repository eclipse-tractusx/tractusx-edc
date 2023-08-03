package org.eclipse.tractusx.edc.api.bpn;

import org.eclipse.edc.connector.api.management.configuration.ManagementApiConfiguration;
import org.eclipse.edc.connector.api.management.configuration.transform.ManagementApiTypeTransformerRegistry;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.tractusx.edc.validation.businesspartner.spi.BusinessPartnerStore;

import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_NAMESPACE;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_PREFIX;

@Extension(value = "Registers the Business Partner Group API")
public class BusinessPartnerGroupApiExtension implements ServiceExtension {

    @Inject
    private WebService webService;
    @Inject
    private ManagementApiConfiguration apiConfiguration;
    @Inject
    private ManagementApiTypeTransformerRegistry transformerRegistry;
    @Inject
    private JsonLd jsonLdService;
    @Inject
    private BusinessPartnerStore businessPartnerStore;

    @Override
    public void initialize(ServiceExtensionContext context) {
        jsonLdService.registerNamespace(TX_PREFIX, TX_NAMESPACE);

        webService.registerResource(apiConfiguration.getContextAlias(), new BusinessPartnerGroupApiController(businessPartnerStore));

    }
}
