/*
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft
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
 */

package org.eclipse.tractusx.edc.identity.mapper;

import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.tractusx.edc.identity.mapper.BdrsClientExtension.BDRS_SERVER_URL_PROPERTY;
import static org.eclipse.tractusx.edc.identity.mapper.BdrsClientExtension.CONNECTOR_DID_PROPERTY;
import static org.eclipse.tractusx.edc.identity.mapper.BdrsClientExtension.CREDENTIAL_SERVICE_BASE_URL_PROPERTY;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class BdrsClientImplExtensionTest {

    private final Monitor monitor = mock();
    private final DidResolverRegistry resolverRegistry = mock();

    @BeforeEach
    void setup(ServiceExtensionContext context) {
        context.registerService(Monitor.class, monitor);
        context.registerService(DidResolverRegistry.class, resolverRegistry);
    }

    @Test
    void createClient_whenUrlMissing_expectException(ServiceExtensionContext context, BdrsClientExtension extension) {
        var cfg = mock(Config.class);
        when(cfg.getString(eq(BDRS_SERVER_URL_PROPERTY))).thenThrow(new EdcException(BDRS_SERVER_URL_PROPERTY));
        when(context.getConfig()).thenReturn(cfg);
        when(cfg.getString(eq(BDRS_SERVER_URL_PROPERTY), isNull())).thenReturn(null);
        when(cfg.getString(eq(CONNECTOR_DID_PROPERTY), isNull())).thenReturn("did:web:self");
        when(cfg.getString(eq(CREDENTIAL_SERVICE_BASE_URL_PROPERTY), isNull())).thenReturn("https://credential.service");

        assertThatThrownBy(() -> extension.getBdrsClient(context)).isInstanceOf(EdcException.class)
                .hasMessageContaining(BDRS_SERVER_URL_PROPERTY);
    }

    @Test
    void createClient_whenNoCredentialServiceUrl_shouldInvokeResolver(ServiceExtensionContext context, BdrsClientExtension extension) {
        var cfg = mock(Config.class);
        when(context.getConfig()).thenReturn(cfg);
        when(cfg.getString(eq(BDRS_SERVER_URL_PROPERTY), isNull())).thenReturn("https://bdrs.server");
        when(cfg.getString(eq(CREDENTIAL_SERVICE_BASE_URL_PROPERTY), isNull())).thenReturn(null);
        when(cfg.getString(eq(CONNECTOR_DID_PROPERTY), isNull())).thenReturn("did:web:self");
        when(resolverRegistry.resolve(anyString())).thenReturn(Result.success(DidDocument.Builder.newInstance().service(List.of(new Service(null, "CredentialService", "http://credential.service"))).build()));

        extension.getBdrsClient(context);

        verify(monitor).withPrefix(anyString());
        verify(monitor).warning("No config value found for 'tx.edc.iam.iatp.credentialservice.url'. As a fallback, the credentialService URL from this connector's DID document will be resolved.");
        verifyNoMoreInteractions(monitor);
    }

    @Test
    void createClient_whenResolverFails_expectLogError(ServiceExtensionContext context, BdrsClientExtension extension) {
        var cfg = mock(Config.class);
        when(context.getConfig()).thenReturn(cfg);
        when(cfg.getString(eq(BDRS_SERVER_URL_PROPERTY), isNull())).thenReturn("https://bdrs.server");
        when(cfg.getString(eq(CREDENTIAL_SERVICE_BASE_URL_PROPERTY), isNull())).thenReturn(null);
        when(cfg.getString(eq(CONNECTOR_DID_PROPERTY), isNull())).thenReturn("did:web:self");
        when(resolverRegistry.resolve(anyString())).thenReturn(Result.failure("test failure"));

        var client = extension.getBdrsClient(context);

        verify(monitor).withPrefix(anyString());
        verify(monitor).warning("No config value found for 'tx.edc.iam.iatp.credentialservice.url'. As a fallback, the credentialService URL from this connector's DID document will be resolved.");

        // the DID url resolver is only invoked on-demand, so no eager-loading of the DID document
        verify(monitor, never()).severe("Resolving the credentialService URL failed. This runtime won't be able to communicate with BDRS. Error: test failure.");
        verifyNoMoreInteractions(monitor);
    }

    @Test
    void createClient_whenNoDid_expectLogError(ServiceExtensionContext context, BdrsClientExtension extension) {
        var cfg = mock(Config.class);
        when(context.getConfig()).thenReturn(cfg);
        when(cfg.getString(eq(BDRS_SERVER_URL_PROPERTY), isNull())).thenReturn("https://bdrs.server");
        when(cfg.getString(eq(CREDENTIAL_SERVICE_BASE_URL_PROPERTY), isNull())).thenReturn("https://credential.service");
        when(cfg.getString(eq(CONNECTOR_DID_PROPERTY), isNull())).thenReturn(null);

        assertThatThrownBy(() -> extension.getBdrsClient(context)).isInstanceOf(EdcException.class)
                .hasMessageContaining(CONNECTOR_DID_PROPERTY);
    }
}