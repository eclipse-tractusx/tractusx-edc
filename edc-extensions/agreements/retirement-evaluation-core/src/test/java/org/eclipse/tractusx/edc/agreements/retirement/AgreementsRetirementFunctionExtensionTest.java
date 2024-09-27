package org.eclipse.tractusx.edc.agreements.retirement;


import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.tractusx.edc.agreements.retirement.function.AgreementsRetirementFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


import static org.eclipse.edc.connector.controlplane.contract.spi.validation.ContractValidationService.TRANSFER_SCOPE;
import static org.eclipse.edc.connector.policy.monitor.PolicyMonitorExtension.POLICY_MONITOR_SCOPE;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
class AgreementsRetirementFunctionExtensionTest {

    private final PolicyEngine policyEngine = mock();

    @BeforeEach
    void setup(ServiceExtensionContext context) {
        context.registerService(PolicyEngine.class, policyEngine);
    }

    @Test
    public void verify_functionIsRegisteredOnInitialization(ServiceExtensionContext context, AgreementsRetirementFunctionExtension extension) {
        extension.initialize(context);

        verify(policyEngine, times(1))
                .registerFunction(eq(TRANSFER_SCOPE), eq(Permission.class), any(AgreementsRetirementFunction.class));
        verify(policyEngine, times(1))
                .registerFunction(eq(POLICY_MONITOR_SCOPE), eq(Permission.class), any(AgreementsRetirementFunction.class));
    }
}