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

import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.tractusx.edc.iam.ssi.miw.api.MiwApiClient;
import org.eclipse.tractusx.edc.iam.ssi.miw.oauth2.MiwOauth2ClientConfiguration;
import org.eclipse.tractusx.edc.iam.ssi.miw.oauth2.MiwOauth2ClientImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.iam.ssi.miw.SsiMiwOauth2ClientExtension.CLIENT_ID;
import static org.eclipse.tractusx.edc.iam.ssi.miw.SsiMiwOauth2ClientExtension.CLIENT_SECRET_ALIAS;
import static org.eclipse.tractusx.edc.iam.ssi.miw.SsiMiwOauth2ClientExtension.TOKEN_URL;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
public class SsiMiwOauth2ClientExtensionTest {

    SsiMiwOauth2ClientExtension extension;

    ServiceExtensionContext context;

    Vault vault = mock(Vault.class);

    @BeforeEach
    void setup(ObjectFactory factory, ServiceExtensionContext context) {
        this.context = spy(context);
        context.registerService(MiwApiClient.class, mock(MiwApiClient.class));
        context.registerService(TypeManager.class, new TypeManager());
        context.registerService(Vault.class, vault);
        extension = factory.constructInstance(SsiMiwOauth2ClientExtension.class);
    }

    @Test
    void initialize() {
        var config = mock(Config.class);
        when(context.getConfig()).thenReturn(config);
        when(config.getString(TOKEN_URL)).thenReturn("url");
        when(config.getString(CLIENT_ID)).thenReturn("clientId");
        when(config.getString(CLIENT_SECRET_ALIAS)).thenReturn("clientSecretAlias");
        when(vault.resolveSecret("clientSecretAlias")).thenReturn("clientSecret");

        assertThat(extension.oauth2Client(context)).isInstanceOf(MiwOauth2ClientImpl.class);
        verify(config).getString(TOKEN_URL);
        verify(config).getString(CLIENT_ID);
        verify(config).getString(CLIENT_SECRET_ALIAS);
        verify(vault).resolveSecret("clientSecretAlias");
    }

    @Test
    void initialize_withTrailingUrl() {
        var config = mock(Config.class);
        when(context.getConfig()).thenReturn(config);
        when(config.getString(TOKEN_URL)).thenReturn("http://localhost:8080/");
        when(config.getString(CLIENT_ID)).thenReturn("clientId");
        when(config.getString(CLIENT_SECRET_ALIAS)).thenReturn("clientSecretAlias");
        when(vault.resolveSecret("clientSecretAlias")).thenReturn("clientSecret");

        assertThat(extension.oauth2Client(context))
                .asInstanceOf(InstanceOfAssertFactories.type(MiwOauth2ClientImpl.class))
                .extracting(MiwOauth2ClientImpl::getConfiguration)
                .extracting(MiwOauth2ClientConfiguration::getTokenUrl)
                .isEqualTo("http://localhost:8080");
        
        verify(config).getString(TOKEN_URL);
        verify(config).getString(CLIENT_ID);
        verify(config).getString(CLIENT_SECRET_ALIAS);
        verify(vault).resolveSecret("clientSecretAlias");
    }


}
