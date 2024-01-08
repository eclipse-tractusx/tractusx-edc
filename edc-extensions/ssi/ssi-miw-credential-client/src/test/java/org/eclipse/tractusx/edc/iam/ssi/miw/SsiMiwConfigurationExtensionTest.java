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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.iam.ssi.miw.SsiMiwConfigurationExtension.AUTHORITY_ID_TEMPLATE;
import static org.eclipse.tractusx.edc.iam.ssi.miw.SsiMiwConfigurationExtension.MIW_AUTHORITY_ID;
import static org.eclipse.tractusx.edc.iam.ssi.miw.SsiMiwConfigurationExtension.MIW_AUTHORITY_ISSUER;
import static org.eclipse.tractusx.edc.iam.ssi.miw.SsiMiwConfigurationExtension.MIW_BASE_URL;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
public class SsiMiwConfigurationExtensionTest {

    private SsiMiwConfigurationExtension extension;

    private ServiceExtensionContext context;

    @BeforeEach
    void setup(ObjectFactory factory, ServiceExtensionContext context) {
        this.context = context;
        extension = factory.constructInstance(SsiMiwConfigurationExtension.class);
    }

    @Test
    void initialize() {
        var url = "http://localhost:8080";
        var authorityId = "id";
        var authorityIssuer = "issuer";

        var cfg = mock(Config.class);
        when(context.getConfig()).thenReturn(cfg);

        when(cfg.getString(MIW_BASE_URL)).thenReturn(url);
        when(cfg.getString(MIW_AUTHORITY_ID)).thenReturn(authorityId);
        when(cfg.getString(eq(MIW_AUTHORITY_ISSUER), anyString())).thenReturn(authorityIssuer);

        var miwConfig = extension.miwConfiguration(context);

        verify(cfg).getString(MIW_BASE_URL);
        verify(cfg).getString(MIW_AUTHORITY_ID);
        verify(cfg).getString(eq(MIW_AUTHORITY_ISSUER), anyString());

        assertThat(miwConfig.getUrl()).isEqualTo(url);
        assertThat(miwConfig.getAuthorityId()).isEqualTo(authorityId);
        assertThat(miwConfig.getAuthorityIssuer()).isEqualTo(authorityIssuer);

    }

    @Test
    void initialize_withDefaultIssuer() {
        var url = "http://localhost:8080";
        var authorityId = "id";

        var cfg = mock(Config.class);
        when(context.getConfig()).thenReturn(cfg);

        when(cfg.getString(MIW_BASE_URL)).thenReturn(url);
        when(cfg.getString(MIW_AUTHORITY_ID)).thenReturn(authorityId);
        when(cfg.getString(eq(MIW_AUTHORITY_ISSUER), anyString())).thenAnswer(answer -> answer.getArgument(1));

        var miwConfig = extension.miwConfiguration(context);

        verify(cfg).getString(MIW_BASE_URL);
        verify(cfg).getString(MIW_AUTHORITY_ID);
        verify(cfg).getString(eq(MIW_AUTHORITY_ISSUER), anyString());

        assertThat(miwConfig.getUrl()).isEqualTo(url);
        assertThat(miwConfig.getAuthorityId()).isEqualTo(authorityId);
        assertThat(miwConfig.getAuthorityIssuer()).isEqualTo(format(AUTHORITY_ID_TEMPLATE, "localhost%3A8080", authorityId));

    }

    @Test
    void initialize_withTrailingUrl() {
        var url = "http://localhost:8080/";
        var authorityId = "id";

        var cfg = mock(Config.class);
        when(context.getConfig()).thenReturn(cfg);

        when(cfg.getString(MIW_BASE_URL)).thenReturn(url);
        when(cfg.getString(MIW_AUTHORITY_ID)).thenReturn(authorityId);
        when(cfg.getString(eq(MIW_AUTHORITY_ISSUER), anyString())).thenAnswer(answer -> answer.getArgument(1));

        var miwConfig = extension.miwConfiguration(context);

        verify(cfg).getString(MIW_BASE_URL);
        verify(cfg).getString(MIW_AUTHORITY_ID);
        verify(cfg).getString(eq(MIW_AUTHORITY_ISSUER), anyString());

        assertThat(miwConfig.getUrl()).isEqualTo("http://localhost:8080");
        assertThat(miwConfig.getAuthorityId()).isEqualTo(authorityId);
        assertThat(miwConfig.getAuthorityIssuer()).isEqualTo(format(AUTHORITY_ID_TEMPLATE, "localhost%3A8080", authorityId));

    }

}
