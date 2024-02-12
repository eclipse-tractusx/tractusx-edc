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

package org.eclipse.tractusx.edc.validation.businesspartner;

import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.tractusx.edc.validation.businesspartner.functions.legacy.BusinessPartnerPermissionFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LegacyBusinessPartnerValidationExtensionTest {

    private LegacyBusinessPartnerValidationExtension extension;

    // mocks
    private ServiceExtensionContext serviceExtensionContext;
    private PolicyEngine policyEngine;
    private RuleBindingRegistry ruleBindingRegistry;

    @BeforeEach
    void setup() {

        policyEngine = mock(PolicyEngine.class);
        ruleBindingRegistry = mock(RuleBindingRegistry.class);

        var monitor = mock(Monitor.class);
        serviceExtensionContext = mock(ServiceExtensionContext.class);

        when(serviceExtensionContext.getMonitor()).thenReturn(monitor);

        extension = new LegacyBusinessPartnerValidationExtension(ruleBindingRegistry, policyEngine);
    }

    @Test
    void testRegisterDutyFunction() {

        // invoke
        extension.initialize(serviceExtensionContext);

        // verify
        verify(policyEngine, times(3))
                .registerFunction(
                        anyString(),
                        eq(Duty.class),
                        eq(LegacyBusinessPartnerValidationExtension.BUSINESS_PARTNER_CONSTRAINT_KEY),
                        any());
    }

    @Test
    void testRegisterPermissionFunction() {

        // invoke
        extension.initialize(serviceExtensionContext);

        // verify
        verify(policyEngine, times(3))
                .registerFunction(
                        anyString(),
                        eq(Permission.class),
                        eq(LegacyBusinessPartnerValidationExtension.BUSINESS_PARTNER_CONSTRAINT_KEY),
                        any());
    }

    @Test
    void testRegisterProhibitionFunction() {

        // invoke
        extension.initialize(serviceExtensionContext);

        // verify
        verify(policyEngine, times(3))
                .registerFunction(
                        anyString(),
                        eq(Prohibition.class),
                        eq(LegacyBusinessPartnerValidationExtension.BUSINESS_PARTNER_CONSTRAINT_KEY),
                        any());
    }

    @Test
    void testLogConfiguration() {

        when(serviceExtensionContext.getSetting(LegacyBusinessPartnerValidationExtension.BUSINESS_PARTNER_VALIDATION_LOG_AGREEMENT_VALIDATION, "true")).thenReturn("false");

        var captor = ArgumentCaptor.forClass(BusinessPartnerPermissionFunction.class);
        // invoke
        extension.initialize(serviceExtensionContext);

        // verify
        verify(policyEngine, times(3))
                .registerFunction(
                        anyString(),
                        eq(Permission.class),
                        eq(LegacyBusinessPartnerValidationExtension.BUSINESS_PARTNER_CONSTRAINT_KEY),
                        captor.capture());

        assertThat(captor.getValue().isLogAgreementEvaluation()).isFalse();
    }
}
