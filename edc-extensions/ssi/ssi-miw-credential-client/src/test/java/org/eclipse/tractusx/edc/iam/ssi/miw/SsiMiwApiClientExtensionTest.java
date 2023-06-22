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

package org.eclipse.tractusx.edc.iam.ssi.miw;

import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.tractusx.edc.iam.ssi.miw.api.MiwApiClient;
import org.eclipse.tractusx.edc.iam.ssi.miw.api.MiwApiClientImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.iam.ssi.miw.SsiMiwApiClientExtension.MIW_AUTHORITY_ID;
import static org.eclipse.tractusx.edc.iam.ssi.miw.SsiMiwApiClientExtension.MIW_BASE_URL;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
public class SsiMiwApiClientExtensionTest {

    SsiMiwApiClientExtension extension;

    ServiceExtensionContext context;

    @BeforeEach
    void setup(ObjectFactory factory, ServiceExtensionContext context) {
        this.context = spy(context);
        context.registerService(MiwApiClient.class, mock(MiwApiClient.class));
        context.registerService(TypeManager.class, new TypeManager());
        extension = factory.constructInstance(SsiMiwApiClientExtension.class);
    }

    @Test
    void initialize() {
        var config = mock(Config.class);
        when(context.getConfig()).thenReturn(config);
        when(config.getString(MIW_BASE_URL)).thenReturn("url");
        when(config.getString(MIW_AUTHORITY_ID)).thenReturn("authorityId");


        assertThat(extension.apiClient(context)).isInstanceOf(MiwApiClientImpl.class);
        verify(config).getString(MIW_BASE_URL);
        verify(config).getString(MIW_AUTHORITY_ID);
    }
}
