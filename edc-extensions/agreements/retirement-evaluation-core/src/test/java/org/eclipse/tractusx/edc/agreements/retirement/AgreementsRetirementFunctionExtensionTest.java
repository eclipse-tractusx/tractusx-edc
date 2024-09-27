package org.eclipse.tractusx.edc.agreements.retirement;


import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.Mockito.mock;

@ExtendWith(DependencyInjectionExtension.class)
class AgreementsRetirementFunctionExtensionTest {

    private final PolicyEngine policyEngine = mock();
    private final RuleBindingRegistry ruleBindingRegistry = mock();

    @BeforeEach
    void setup(ServiceExtensionContext context) {
        context.registerService(PolicyEngine.class, policyEngine);
        context.registerService(RuleBindingRegistry.class, ruleBindingRegistry);
    }

    @BeforeEach
    public void setUp() {

    }
}