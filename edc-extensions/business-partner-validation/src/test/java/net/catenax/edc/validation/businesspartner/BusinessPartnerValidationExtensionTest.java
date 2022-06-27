/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Initial API and Implementation
 *
 */

package net.catenax.edc.validation.businesspartner;

import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.PolicyEngine;
import org.eclipse.dataspaceconnector.spi.policy.RuleBindingRegistry;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BusinessPartnerValidationExtensionTest {

  private BusinessPartnerValidationExtension extension;

  // mocks
  private ServiceExtensionContext serviceExtensionContext;
  private PolicyEngine policyEngine;

  @BeforeEach
  void setup() {

    policyEngine = Mockito.mock(PolicyEngine.class);
    RuleBindingRegistry ruleBindingRegistry = Mockito.mock(RuleBindingRegistry.class);

    final Monitor monitor = Mockito.mock(Monitor.class);
    serviceExtensionContext = Mockito.mock(ServiceExtensionContext.class);

    Mockito.when(serviceExtensionContext.getMonitor()).thenReturn(monitor);
    Mockito.when(serviceExtensionContext.getService(PolicyEngine.class)).thenReturn(policyEngine);
    Mockito.when(serviceExtensionContext.getService(RuleBindingRegistry.class))
        .thenReturn(ruleBindingRegistry);

    extension = new BusinessPartnerValidationExtension();
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
