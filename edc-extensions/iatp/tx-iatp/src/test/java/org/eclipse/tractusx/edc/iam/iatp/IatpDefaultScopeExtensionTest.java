/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.iam.iatp;

import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.tractusx.edc.iam.iatp.scope.DefaultScopeExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.tractusx.edc.iam.iatp.IatpDefaultScopeExtension.CATALOG_REQUEST_SCOPE;
import static org.eclipse.tractusx.edc.iam.iatp.IatpDefaultScopeExtension.NEGOTIATION_REQUEST_SCOPE;
import static org.eclipse.tractusx.edc.iam.iatp.IatpDefaultScopeExtension.TRANSFER_PROCESS_REQUEST_SCOPE;
import static org.eclipse.tractusx.edc.iam.iatp.IatpDefaultScopeExtension.TX_IATP_DEFAULT_SCOPE_PREFIX;
import static org.eclipse.tractusx.edc.iam.iatp.TxIatpConstants.DEFAULT_SCOPES;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
public class IatpDefaultScopeExtensionTest {

    private final PolicyEngine policyEngine = mock();

    @BeforeEach
    void setup(ServiceExtensionContext context) {
        context.registerService(PolicyEngine.class, policyEngine);
    }

    @Test
    void initialize(ServiceExtensionContext context, IatpDefaultScopeExtension extension) {
        extension.initialize(context);

        verify(policyEngine).registerPostValidator(eq(CATALOG_REQUEST_SCOPE), argThat(new ScopeMatcher(DEFAULT_SCOPES)));
        verify(policyEngine).registerPostValidator(eq(NEGOTIATION_REQUEST_SCOPE), argThat(new ScopeMatcher(DEFAULT_SCOPES)));
        verify(policyEngine).registerPostValidator(eq(TRANSFER_PROCESS_REQUEST_SCOPE), argThat(new ScopeMatcher(DEFAULT_SCOPES)));
    }

    @Test
    void initialize_withConfiguredScopes(ServiceExtensionContext context, IatpDefaultScopeExtension extension) {
        var cfg = ConfigFactory.fromMap(Map.of(
                "foo.alias", "org.test.alias.foo",
                "foo.type", "FooCredential",
                "foo.operation", "read",
                "bar.alias", "org.test.alias.bar",
                "bar.type", "BarCredential",
                "bar.operation", "write"
        ));
        when(context.getConfig(TX_IATP_DEFAULT_SCOPE_PREFIX)).thenReturn(cfg);
        extension.initialize(context);

        var expectedScopes = Set.of("org.test.alias.foo:FooCredential:read", "org.test.alias.bar:BarCredential:write");

        verify(policyEngine).registerPostValidator(eq(CATALOG_REQUEST_SCOPE), argThat(new ScopeMatcher(expectedScopes)));
        verify(policyEngine).registerPostValidator(eq(NEGOTIATION_REQUEST_SCOPE), argThat(new ScopeMatcher(expectedScopes)));
        verify(policyEngine).registerPostValidator(eq(TRANSFER_PROCESS_REQUEST_SCOPE), argThat(new ScopeMatcher(expectedScopes)));
    }

    @Test
    void initialize_fails_withBadConfiguredScopes(ServiceExtensionContext context, IatpDefaultScopeExtension extension) {
        var cfg = ConfigFactory.fromMap(Map.of(
                "foo.alias", "org.test.alias.foo"
        ));
        when(context.getConfig(TX_IATP_DEFAULT_SCOPE_PREFIX)).thenReturn(cfg);

        assertThatThrownBy(() -> extension.initialize(context)).isInstanceOf(EdcException.class);
    }

    private record ScopeMatcher(Set<String> scopes) implements ArgumentMatcher<DefaultScopeExtractor> {

        @Override
        public boolean matches(DefaultScopeExtractor defaultScopeExtractor) {
            return defaultScopeExtractor.defaultScopes().containsAll(scopes);
        }
    }
}
