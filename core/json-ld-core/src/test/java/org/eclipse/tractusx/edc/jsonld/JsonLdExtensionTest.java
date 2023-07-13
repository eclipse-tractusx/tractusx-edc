/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.jsonld;

import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.eclipse.tractusx.edc.jsonld.JsonLdExtension.CREDENTIALS_SUMMARY_V_1;
import static org.eclipse.tractusx.edc.jsonld.JsonLdExtension.CREDENTIALS_V_1;
import static org.eclipse.tractusx.edc.jsonld.JsonLdExtension.SECURITY_ED25519_V1;
import static org.eclipse.tractusx.edc.jsonld.JsonLdExtension.SECURITY_JWS_V1;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;

@ExtendWith(DependencyInjectionExtension.class)
public class JsonLdExtensionTest {

    JsonLdExtension extension;

    JsonLd jsonLdService = mock(JsonLd.class);

    @BeforeEach
    void setup(ObjectFactory factory, ServiceExtensionContext context) {
        context.registerService(JsonLd.class, jsonLdService);
        extension = factory.constructInstance(JsonLdExtension.class);
    }

    @Test
    void initialize(ServiceExtensionContext context) {
        extension.initialize(context);
        jsonLdService.registerCachedDocument(eq(CREDENTIALS_V_1), any());
        jsonLdService.registerCachedDocument(eq(CREDENTIALS_SUMMARY_V_1), any());
        jsonLdService.registerCachedDocument(eq(SECURITY_JWS_V1), any());
        jsonLdService.registerCachedDocument(eq(SECURITY_ED25519_V1), any());

    }
}
