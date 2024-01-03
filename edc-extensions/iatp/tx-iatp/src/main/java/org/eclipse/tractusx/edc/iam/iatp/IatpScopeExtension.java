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

package org.eclipse.tractusx.edc.iam.iatp;

import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.tractusx.edc.iam.iatp.scope.DefaultScopeExtractor;

import java.util.Set;

import static org.eclipse.edc.iam.identitytrust.core.IatpScopeExtractorExtension.CATALOG_REQUEST_SCOPE;
import static org.eclipse.edc.iam.identitytrust.core.IatpScopeExtractorExtension.NEGOTIATION_REQUEST_SCOPE;
import static org.eclipse.edc.iam.identitytrust.core.IatpScopeExtractorExtension.TRANSFER_PROCESS_REQUEST_SCOPE;
import static org.eclipse.tractusx.edc.iam.iatp.IatpScopeExtension.NAME;

@Extension(NAME)
public class IatpScopeExtension implements ServiceExtension {
    
    static final String NAME = "Tractusx scope mapping extension";
    static final Set<String> DEFAULT_SCOPES = Set.of("org.eclipse.tractusx.vc.type:MembershipCredential:read");
    @Inject
    private PolicyEngine policyEngine;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var contextMappingFunction = new DefaultScopeExtractor(DEFAULT_SCOPES);
        policyEngine.registerPostValidator(CATALOG_REQUEST_SCOPE, contextMappingFunction);
        policyEngine.registerPostValidator(NEGOTIATION_REQUEST_SCOPE, contextMappingFunction);
        policyEngine.registerPostValidator(TRANSFER_PROCESS_REQUEST_SCOPE, contextMappingFunction);
    }
}
