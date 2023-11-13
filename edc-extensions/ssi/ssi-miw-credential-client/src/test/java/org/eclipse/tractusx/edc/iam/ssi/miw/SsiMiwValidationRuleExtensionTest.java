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
import org.eclipse.tractusx.edc.iam.ssi.miw.config.SsiMiwConfiguration;
import org.eclipse.tractusx.edc.iam.ssi.miw.rule.SsiCredentialIssuerValidationRule;
import org.eclipse.tractusx.edc.iam.ssi.miw.rule.SsiCredentialSubjectIdValidationRule;
import org.eclipse.tractusx.edc.iam.ssi.spi.SsiValidationRuleRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Set;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
public class SsiMiwValidationRuleExtensionTest {

    private final SsiValidationRuleRegistry registry = mock(SsiValidationRuleRegistry.class);
    private final SsiMiwConfiguration cfg = mock(SsiMiwConfiguration.class);
    private SsiMiwValidationRuleExtension extension;

    @BeforeEach
    void setup(ObjectFactory factory, ServiceExtensionContext context) {
        context.registerService(SsiMiwConfiguration.class, cfg);
        context.registerService(SsiValidationRuleRegistry.class, registry);
        extension = factory.constructInstance(SsiMiwValidationRuleExtension.class);
    }

    @Test
    void initialize(ServiceExtensionContext context) {
        when(cfg.getAuthorityIssuers()).thenReturn(Set.of("issuer"));

        extension.initialize(context);
        verify(registry).addRule(isA(SsiCredentialSubjectIdValidationRule.class));
        verify(registry).addRule(isA(SsiCredentialIssuerValidationRule.class));

        verify(cfg).getAuthorityIssuers();
    }

}
