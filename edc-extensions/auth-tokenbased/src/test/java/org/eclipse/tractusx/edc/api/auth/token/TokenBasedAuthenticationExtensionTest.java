/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH -  initial implementation
 *
 */

package org.eclipse.tractusx.edc.api.auth.token;

import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
public class TokenBasedAuthenticationExtensionTest {

    private static final String AUTH_SETTING_APIKEY = "edc.api.auth.key";
    private static final String AUTH_SETTING_APIKEY_ALIAS = "edc.api.auth.key.alias";
    private static final String VAULT_KEY = "foo";

    private final Vault vault = mock();

    @BeforeEach
    void setup(ServiceExtensionContext context) {
        context.registerService(Vault.class, vault);

        when(vault.resolveSecret(VAULT_KEY)).thenReturn("foo");
    }

    @Test
    public void testPrimaryMethod_loadKeyFromVault(ServiceExtensionContext context, TokenBasedAuthenticationExtension extension) {
        when(context.getSetting(eq(AUTH_SETTING_APIKEY_ALIAS), isNull())).thenReturn(VAULT_KEY);
        when(context.getSetting(eq(AUTH_SETTING_APIKEY), anyString())).thenReturn("bar");

        extension.initialize(context);

        verify(context, never())
                .getSetting(eq(AUTH_SETTING_APIKEY), anyString());

        verify(context)
                .getSetting(AUTH_SETTING_APIKEY_ALIAS, null);

        verify(vault).resolveSecret(VAULT_KEY);
    }

    @Test
    public void testSecondaryMethod_loadKeyFromConfig(ServiceExtensionContext context, TokenBasedAuthenticationExtension extension) {
        when(context.getSetting(eq(AUTH_SETTING_APIKEY_ALIAS), isNull())).thenReturn(null);
        when(context.getSetting(eq(AUTH_SETTING_APIKEY), anyString())).thenReturn("bar");

        extension.initialize(context);

        verify(context)
                .getSetting(eq(AUTH_SETTING_APIKEY), anyString());

        verify(context)
                .getSetting(AUTH_SETTING_APIKEY_ALIAS, null);

        verify(vault, never()).resolveSecret(anyString());
    }

}
