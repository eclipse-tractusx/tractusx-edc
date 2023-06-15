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

package org.eclipse.tractusx.edc.iam.ssi.identity;

import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.eclipse.tractusx.edc.iam.ssi.spi.SsiCredentialClient;
import org.eclipse.tractusx.edc.iam.ssi.spi.SsiValidationRuleRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@ExtendWith(DependencyInjectionExtension.class)
public class SsiIdentityServiceExtensionTest {

    SsiIdentityServiceExtension extension;

    ServiceExtensionContext context;

    @BeforeEach
    void setup(ObjectFactory factory, ServiceExtensionContext context) {
        this.context = spy(context);
        context.registerService(SsiCredentialClient.class, mock(SsiCredentialClient.class));
        extension = factory.constructInstance(SsiIdentityServiceExtension.class);
    }
    
    @Test
    void initialize() {
        extension.initialize(context);

        assertThat(context.getService(IdentityService.class)).isNotNull().isInstanceOf(SsiIdentityService.class);
        assertThat(context.getService(SsiValidationRuleRegistry.class)).isNotNull().isInstanceOf(SsiValidationRulesRegistryImpl.class);
    }
}
