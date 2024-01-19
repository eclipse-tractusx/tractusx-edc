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

package org.eclipse.tractusx.edc.iam.ssi.miw;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.tractusx.edc.iam.ssi.miw.config.SsiMiwConfiguration;
import org.eclipse.tractusx.edc.iam.ssi.miw.rule.SsiCredentialIssuerValidationRule;
import org.eclipse.tractusx.edc.iam.ssi.miw.rule.SsiCredentialSubjectIdValidationRule;

import static org.eclipse.tractusx.edc.iam.ssi.spi.SsiConstants.SSI_TOKEN_CONTEXT;

@Extension(SsiMiwValidationRuleExtension.EXTENSION_NAME)
public class SsiMiwValidationRuleExtension implements ServiceExtension {

    protected static final String EXTENSION_NAME = "SSI MIW validation rules extension";
    @Inject
    private TokenValidationRulesRegistry registry;

    @Inject
    private Monitor monitor;

    @Inject
    private SsiMiwConfiguration miwConfiguration;

    @Override
    public String name() {
        return EXTENSION_NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        registry.addRule(SSI_TOKEN_CONTEXT, new SsiCredentialSubjectIdValidationRule(monitor));
        registry.addRule(SSI_TOKEN_CONTEXT, new SsiCredentialIssuerValidationRule(miwConfiguration.getAuthorityIssuer(), monitor));
    }
}
