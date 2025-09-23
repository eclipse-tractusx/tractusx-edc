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

package org.eclipse.tractusx.edc.iam.iatp.scope;

import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.policy.context.request.spi.RequestPolicyContext;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.RequestContext;
import org.eclipse.edc.spi.iam.RequestScope;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp08Constants.DSP_SCOPE_V_08;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DSP_SCOPE_V_2025_1;

public class DefaultScopeExtractorTest {

    public static final String DSP_08 = "dataspace-protocol-http";
    private static final Set<String> SCOPES = Set.of("scope1", "scope2");
    private static final Set<String> SCOPES_V08 = Set.of("scope");
    private DefaultScopeExtractor<TestRequestPolicyContext> extractor;

    @BeforeEach
    void setup() {
        var source = new HashMap<String, Set<String>>();
        source.put(DSP_SCOPE_V_08, SCOPES_V08);
        source.put(DSP_SCOPE_V_2025_1, SCOPES);
        extractor = new DefaultScopeExtractor<>(source);
    }

    @Test
    void verify_applyExtractor() {
        var builder = RequestScope.Builder.newInstance();
        var context = new TestRequestPolicyContext(null, builder);

        var result = extractor.apply(Policy.Builder.newInstance().build(), context);

        assertThat(result).isTrue();
        assertThat(builder.build().getScopes()).containsAll(SCOPES);
    }

    @Test
    void verify_applyExtractor_v08() {
        var builder = RequestScope.Builder.newInstance();
        RemoteMessage remoteMessage = CatalogRequestMessage.Builder.newInstance().protocol(DSP_08).build();
        RequestContext requestContext =  RequestContext.Builder.newInstance().message(remoteMessage).build();
        var context = new TestRequestPolicyContext(requestContext, builder);

        var result = extractor.apply(Policy.Builder.newInstance().build(), context);

        assertThat(result).isTrue();
        assertThat(builder.build().getScopes()).containsAll(SCOPES_V08);
    }

    @Test
    void verify_applyExtractorFails_whenTokenParamsBuilderMissing() {
        var context = new TestRequestPolicyContext(null, null);

        assertThatThrownBy(() -> extractor.apply(Policy.Builder.newInstance().build(), context))
                .isInstanceOf(EdcException.class)
                .hasMessageContaining("not set in policy context");

    }

    private static class TestRequestPolicyContext extends RequestPolicyContext {

        protected TestRequestPolicyContext(RequestContext requestContext, RequestScope.Builder requestScopeBuilder) {
            super(requestContext, requestScopeBuilder);
        }

        @Override
        public String scope() {
            return "request.any";
        }
    }
}
