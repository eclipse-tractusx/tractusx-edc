/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.iatp;

import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.net.URISyntaxException;

@Extension("Credentials JSON LD extension")
public class CredentialsJsonLdExtension implements ServiceExtension {

    public static final String BUSINESS_PARTNER_DATA = "https://catenax-ng.github.io/product-core-schemas/businessPartnerData.json";

    @Inject
    private JsonLd jsonLd;
    
    @Override
    public void initialize(ServiceExtensionContext context) {

        try {
            jsonLd.registerCachedDocument(BUSINESS_PARTNER_DATA, Thread.currentThread().getContextClassLoader().getResource("cx-credentials-context.json").toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
