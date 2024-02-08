/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.tractusx.edc.iam.ssi.miw;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
public class SsiMiwOauth2ClientExtensionTest {

    Vault vault = mock(Vault.class);

    @BeforeEach
    void setup(ServiceExtensionContext context) {
        context.registerService(MiwApiClient.class, mock(MiwApiClient.class));
        context.registerService(TypeManager.class, new TypeManager());
        context.registerService(Vault.class, vault);
    }

    @Test
    void initialize(ServiceExtensionContext context, SsiMiwOauth2ClientExtension extension) {
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
    void initialize_withTrailingUrl(ServiceExtensionContext context, SsiMiwOauth2ClientExtension extension) {
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
