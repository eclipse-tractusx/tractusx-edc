/*
 * Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.edc.validation.businesspartner;

import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BusinessPartnerValidationExtensionTest {

  private BusinessPartnerValidationExtension extension;

  // mocks
  private ServiceExtensionContext serviceExtensionContext;
  private PolicyEngine policyEngine;
  private RuleBindingRegistry ruleBindingRegistry;

  @BeforeEach
  void setup() {

    policyEngine = Mockito.mock(PolicyEngine.class);
    ruleBindingRegistry = Mockito.mock(RuleBindingRegistry.class);

    final Monitor monitor = Mockito.mock(Monitor.class);
    serviceExtensionContext = Mockito.mock(ServiceExtensionContext.class);

    Mockito.when(serviceExtensionContext.getMonitor()).thenReturn(monitor);

    extension = new BusinessPartnerValidationExtension(ruleBindingRegistry, policyEngine);
  }

  @Test
  void testRegisterDutyFunction() {

    // invoke
    extension.initialize(serviceExtensionContext);

    // verify
    Mockito.verify(policyEngine, Mockito.times(1))
        .registerFunction(
            Mockito.anyString(),
            Mockito.eq(Duty.class),
            Mockito.eq(BusinessPartnerValidationExtension.BUSINESS_PARTNER_CONSTRAINT_KEY),
            Mockito.any());
  }

  @Test
  void testRegisterPermissionFunction() {

    // invoke
    extension.initialize(serviceExtensionContext);

    // verify
    Mockito.verify(policyEngine, Mockito.times(1))
        .registerFunction(
            Mockito.anyString(),
            Mockito.eq(Permission.class),
            Mockito.eq(BusinessPartnerValidationExtension.BUSINESS_PARTNER_CONSTRAINT_KEY),
            Mockito.any());
  }

  @Test
  void testRegisterProhibitionFunction() {

    // invoke
    extension.initialize(serviceExtensionContext);

    // verify
    Mockito.verify(policyEngine, Mockito.times(1))
        .registerFunction(
            Mockito.anyString(),
            Mockito.eq(Prohibition.class),
            Mockito.eq(BusinessPartnerValidationExtension.BUSINESS_PARTNER_CONSTRAINT_KEY),
            Mockito.any());
  }
}
