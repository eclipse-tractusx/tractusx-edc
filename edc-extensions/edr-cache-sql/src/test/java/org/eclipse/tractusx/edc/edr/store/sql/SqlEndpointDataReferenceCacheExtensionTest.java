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

package org.eclipse.tractusx.edc.edr.store.sql;

import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.edr.store.sql.SqlEndpointDataReferenceCacheExtension.DATASOURCE_SETTING_NAME;
import static org.eclipse.tractusx.edc.edr.store.sql.SqlEndpointDataReferenceCacheExtension.DEFAULT_DATASOURCE_NAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
public class SqlEndpointDataReferenceCacheExtensionTest {

    SqlEndpointDataReferenceCacheExtension extension;
    ServiceExtensionContext context;


    @BeforeEach
    void setUp(ObjectFactory factory, ServiceExtensionContext context) {
        this.context = spy(context);
        context.registerService(TypeManager.class, new TypeManager());
        context.registerService(DataSourceRegistry.class, mock(DataSourceRegistry.class));
        extension = factory.constructInstance(SqlEndpointDataReferenceCacheExtension.class);
    }

    @Test
    void shouldInitializeTheStore() {
        var config = mock(Config.class);
        when(context.getConfig()).thenReturn(config);
        when(config.getString(any(), any())).thenReturn(DEFAULT_DATASOURCE_NAME);

        assertThat(extension.edrCache(context)).isInstanceOf(SqlEndpointDataReferenceCache.class);

        verify(config).getString(DATASOURCE_SETTING_NAME, DEFAULT_DATASOURCE_NAME);
    }
}
