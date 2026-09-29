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
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;

import static org.eclipse.tractusx.edc.identity.mapper.BdrsClientExtension.BDRS_SERVER_URL_PROPERTY;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class BdrsClientImplExtensionTest {

    private final Monitor monitor = mock();
    private final DidResolverRegistry resolverRegistry = mock();
    private final SingleParticipantContextSupplier participantContextSupplier = mock(SingleParticipantContextSupplier.class);

    @BeforeEach
    void setup(ServiceExtensionContext context) {
        context.registerService(Monitor.class, monitor);
        context.registerService(DidResolverRegistry.class, resolverRegistry);
        context.registerService(SingleParticipantContextSupplier.class, participantContextSupplier);
        var config = ConfigFactory.fromMap(Map.of(BDRS_SERVER_URL_PROPERTY, "https://bdrs.server"));
        when(context.getConfig()).thenReturn(config);
    }

    @Test
    void createClient_whenNoCredentialServiceUrl_shouldInvokeResolver(ServiceExtensionContext context, BdrsClientExtension extension) {
        when(resolverRegistry.resolve(anyString())).thenReturn(Result.success(DidDocument.Builder.newInstance().service(List.of(new Service(null, "CredentialService", "http://credential.service"))).build()));
        when(participantContextSupplier.get()).thenReturn(ServiceResult.success(ParticipantContext.Builder.newInstance().identity("did:example:123").participantContextId("uuid").build()));

        extension.getBdrsClient(context);

        verify(monitor).withPrefix(anyString());
        verify(monitor).warning("No config value found for 'tx.edc.iam.dcp.credentialservice.url'. As a fallback, the credentialService URL from this connector's DID document will be resolved.");
    }

    @Test
    void createClient_whenResolverFails_expectLogError(ServiceExtensionContext context, BdrsClientExtension extension) {
        when(resolverRegistry.resolve(anyString())).thenReturn(Result.failure("test failure"));
        when(participantContextSupplier.get()).thenReturn(ServiceResult.success(ParticipantContext.Builder.newInstance().identity("did:example:123").participantContextId("uuid").build()));

        var client = extension.getBdrsClient(context);

        verify(monitor).withPrefix(anyString());
        verify(monitor).warning("No config value found for 'tx.edc.iam.dcp.credentialservice.url'. As a fallback, the credentialService URL from this connector's DID document will be resolved.");

        // the DID url resolver is only invoked on-demand, so no eager-loading of the DID document
        verify(monitor, never()).severe("Resolving the credentialService URL failed. This runtime won't be able to communicate with BDRS. Error: test failure.");
    }
}