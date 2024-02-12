/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.iam.ssi.miw;

import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
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

    @BeforeEach
    void setup(ServiceExtensionContext context) {
        context.registerService(SsiMiwConfiguration.class, cfg);
        context.registerService(TokenValidationRulesRegistry.class, registry);
    }

    @Test
    void initialize(ServiceExtensionContext context, SsiMiwValidationRuleExtension extension) {
        when(cfg.getAuthorityIssuer()).thenReturn("issuer");

        extension.initialize(context);
        verify(registry).addRule(eq(SSI_TOKEN_CONTEXT), isA(SsiCredentialSubjectIdValidationRule.class));
        verify(registry).addRule(eq(SSI_TOKEN_CONTEXT), isA(SsiCredentialIssuerValidationRule.class));

        verify(cfg).getAuthorityIssuer();
    }

}
