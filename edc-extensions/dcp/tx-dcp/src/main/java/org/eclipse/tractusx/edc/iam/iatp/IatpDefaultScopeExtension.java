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

import org.eclipse.edc.policy.context.request.spi.RequestCatalogPolicyContext;
import org.eclipse.edc.policy.context.request.spi.RequestContractNegotiationPolicyContext;
import org.eclipse.edc.policy.context.request.spi.RequestTransferProcessPolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.tractusx.edc.iam.iatp.scope.DefaultScopeExtractor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp08Constants.DSP_SCOPE_V_08;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DSP_SCOPE_V_2025_1;
import static org.eclipse.tractusx.edc.TxIatpConstants.DEFAULT_SCOPES;
import static org.eclipse.tractusx.edc.TxIatpConstants.V08_DEFAULT_SCOPES;
import static org.eclipse.tractusx.edc.iam.iatp.IatpDefaultScopeExtension.NAME;

@Extension(NAME)
public class IatpDefaultScopeExtension implements ServiceExtension {

    public static final String TX_IATP_DEFAULT_SCOPE_PREFIX = "tx.edc.iam.iatp.default-scopes";

    public static final String TX_IATP_DEFAULT_SCOPE_PREFIX_CONFIG_ALIAS = TX_IATP_DEFAULT_SCOPE_PREFIX + ".<scopeAlias>.";

    @Setting(context = TX_IATP_DEFAULT_SCOPE_PREFIX_CONFIG_ALIAS, value = "The alias of the scope e.g. org.eclipse.edc.vc.type", required = true)
    public static final String ALIAS = "alias";

    @Setting(context = TX_IATP_DEFAULT_SCOPE_PREFIX_CONFIG_ALIAS, value = "The alias of the scope e.g. MembershipCredential", required = true)
    public static final String TYPE = "type";

    @Setting(context = TX_IATP_DEFAULT_SCOPE_PREFIX_CONFIG_ALIAS, value = "The alias of the scope e.g. read", required = true)
    public static final String OPERATION = "operation";
    static final String NAME = "Tractusx default scope extension";
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
        policyEngine.registerPostValidator(RequestCatalogPolicyContext.class, new DefaultScopeExtractor<>(defaultScopes(context)));
        policyEngine.registerPostValidator(RequestContractNegotiationPolicyContext.class, new DefaultScopeExtractor<>(defaultScopes(context)));
        policyEngine.registerPostValidator(RequestTransferProcessPolicyContext.class, new DefaultScopeExtractor<>(defaultScopes(context)));
    }

    private Map<String, Set<String>> defaultScopes(ServiceExtensionContext context) {
        var config = context.getConfig(TX_IATP_DEFAULT_SCOPE_PREFIX);
        var scopes = config.partition().map(this::createScope).collect(Collectors.toSet());
        var scopesByVersion = new HashMap<String, Set<String>>();
        if (scopes.isEmpty()) {
            monitor.info(format("No default scope from configuration. Using the default ones %s for %s and %s for %s",
                    DSP_SCOPE_V_2025_1, DEFAULT_SCOPES, DSP_SCOPE_V_08, V08_DEFAULT_SCOPES));
            scopesByVersion.put(DSP_SCOPE_V_08, V08_DEFAULT_SCOPES);
            scopesByVersion.put(DSP_SCOPE_V_2025_1, DEFAULT_SCOPES);
            return scopesByVersion;
        } else {
            scopesByVersion.put(DSP_SCOPE_V_08, scopes);
            scopesByVersion.put(DSP_SCOPE_V_2025_1, scopes);
            return scopesByVersion;
        }
    }

    private String createScope(Config config) {
        var alias = config.getString(ALIAS);
        var type = config.getString(TYPE);
        var operation = config.getString(OPERATION);
        return format("%s:%s:%s", alias, type, operation);
    }
}
