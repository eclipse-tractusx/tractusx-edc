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

import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.tractusx.edc.iam.ssi.miw.config.SsiMiwConfiguration;
import org.eclipse.tractusx.edc.iam.ssi.miw.rule.SsiCredentialIssuerValidationRule;
import org.eclipse.tractusx.edc.iam.ssi.miw.rule.SsiCredentialSubjectIdValidationRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.eclipse.tractusx.edc.iam.ssi.spi.SsiConstants.SSI_TOKEN_CONTEXT;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
public class SsiMiwValidationRuleExtensionTest {

    private final TokenValidationRulesRegistry registry = mock(TokenValidationRulesRegistry.class);
    private final SsiMiwConfiguration cfg = mock(SsiMiwConfiguration.class);
    private SsiMiwValidationRuleExtension extension;

    @BeforeEach
    void setup(ObjectFactory factory, ServiceExtensionContext context) {
        context.registerService(SsiMiwConfiguration.class, cfg);
        context.registerService(TokenValidationRulesRegistry.class, registry);
        extension = factory.constructInstance(SsiMiwValidationRuleExtension.class);
    }

    @Test
    void initialize(ServiceExtensionContext context) {
        when(cfg.getAuthorityIssuer()).thenReturn("issuer");

        extension.initialize(context);
        verify(registry).addRule(eq(SSI_TOKEN_CONTEXT), isA(SsiCredentialSubjectIdValidationRule.class));
        verify(registry).addRule(eq(SSI_TOKEN_CONTEXT), isA(SsiCredentialIssuerValidationRule.class));

        verify(cfg).getAuthorityIssuer();
    }

}
