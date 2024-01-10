/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.iam.iatp;

import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.tractusx.edc.iam.iatp.scope.DefaultScopeExtractor;

import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.eclipse.edc.iam.identitytrust.core.IatpScopeExtractorExtension.CATALOG_REQUEST_SCOPE;
import static org.eclipse.edc.iam.identitytrust.core.IatpScopeExtractorExtension.NEGOTIATION_REQUEST_SCOPE;
import static org.eclipse.edc.iam.identitytrust.core.IatpScopeExtractorExtension.TRANSFER_PROCESS_REQUEST_SCOPE;
import static org.eclipse.tractusx.edc.iam.iatp.IatpDefaultScopeExtension.NAME;
import static org.eclipse.tractusx.edc.iam.iatp.TxIatpConstants.DEFAULT_SCOPES;

@Extension(NAME)
public class IatpDefaultScopeExtension implements ServiceExtension {

    public static final String TX_IATP_DEFAULT_SCOPE_PREFIX = "edc.iam.iatp.default-scopes";

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
        var contextMappingFunction = new DefaultScopeExtractor(defaultScopes(context));
        policyEngine.registerPostValidator(CATALOG_REQUEST_SCOPE, contextMappingFunction);
        policyEngine.registerPostValidator(NEGOTIATION_REQUEST_SCOPE, contextMappingFunction);
        policyEngine.registerPostValidator(TRANSFER_PROCESS_REQUEST_SCOPE, contextMappingFunction);
    }


    private Set<String> defaultScopes(ServiceExtensionContext context) {
        var config = context.getConfig(TX_IATP_DEFAULT_SCOPE_PREFIX);
        var scopes = config.partition().map(this::createScope).collect(Collectors.toSet());

        if (scopes.isEmpty()) {
            monitor.info(format("No default scope from configuration. Using the default ones %s", DEFAULT_SCOPES));
            return DEFAULT_SCOPES;
        } else {
            return scopes;
        }
    }

    private String createScope(Config config) {
        var alias = config.getString(ALIAS);
        var type = config.getString(TYPE);
        var operation = config.getString(OPERATION);
        return format("%s:%s:%s", alias, type, operation);
    }
}
