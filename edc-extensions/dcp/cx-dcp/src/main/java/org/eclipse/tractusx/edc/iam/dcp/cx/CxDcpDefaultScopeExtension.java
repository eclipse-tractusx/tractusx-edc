/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 * Copyright (c) 2026 SAP SE
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

package org.eclipse.tractusx.edc.iam.dcp.cx;

import org.eclipse.edc.policy.context.request.spi.RequestCatalogPolicyContext;
import org.eclipse.edc.policy.context.request.spi.RequestContractNegotiationPolicyContext;
import org.eclipse.edc.policy.context.request.spi.RequestTransferProcessPolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.tractusx.edc.iam.iatp.scope.DefaultScopeExtractor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp08Constants.DSP_SCOPE_V_08;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DSP_SCOPE_V_2025_1;
import static org.eclipse.tractusx.edc.TxIatpConstants.DEFAULT_SCOPES;
import static org.eclipse.tractusx.edc.TxIatpConstants.V08_DEFAULT_SCOPES;
import static org.eclipse.tractusx.edc.iam.dcp.cx.CxDcpDefaultScopeExtension.NAME;
import static org.eclipse.tractusx.edc.iam.iatp.IatpDefaultScopeExtension.TX_IATP_DEFAULT_SCOPE_PREFIX;

@Extension(NAME)
public class CxDcpDefaultScopeExtension implements ServiceExtension {

    static final String NAME = "CX default scope extension";
    @Inject
    private PolicyEngine policyEngine;

    @Inject
    private Monitor monitor;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var defaultScopes = defaultScopes(context);
        if (!defaultScopes.isEmpty()) {
            policyEngine.registerPostValidator(RequestCatalogPolicyContext.class, new DefaultScopeExtractor<>(defaultScopes));
            policyEngine.registerPostValidator(RequestContractNegotiationPolicyContext.class, new DefaultScopeExtractor<>(defaultScopes));
            policyEngine.registerPostValidator(RequestTransferProcessPolicyContext.class, new DefaultScopeExtractor<>(defaultScopes));
        }
    }

    private Map<String, Set<String>> defaultScopes(ServiceExtensionContext context) {
        var config = context.getConfig(TX_IATP_DEFAULT_SCOPE_PREFIX);
        var scopesByVersion = new HashMap<String, Set<String>>();
        if (config.getEntries().isEmpty()) {
            monitor.info(format("No default scope from configuration. Using the default ones %s for %s and %s for %s",
                    DSP_SCOPE_V_2025_1, DEFAULT_SCOPES, DSP_SCOPE_V_08, V08_DEFAULT_SCOPES));
            scopesByVersion.put(DSP_SCOPE_V_08, V08_DEFAULT_SCOPES);
            scopesByVersion.put(DSP_SCOPE_V_2025_1, DEFAULT_SCOPES);
        }
        return scopesByVersion;
    }
}
