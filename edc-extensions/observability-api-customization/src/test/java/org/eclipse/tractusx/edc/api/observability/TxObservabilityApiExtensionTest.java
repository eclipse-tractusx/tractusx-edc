package org.eclipse.tractusx.edc.api.observability;

import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.health.HealthCheckService;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.eclipse.edc.web.spi.WebService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class TxObservabilityApiExtensionTest {

    private TxObservabilityApiExtension extension;
    private ServiceExtensionContext context;
    private HealthCheckService healthCheckService;
    private WebService webService;

    @BeforeEach
    void setUp(ServiceExtensionContext context, ObjectFactory factory) {
        this.context = spy(context);

        healthCheckService = registerMock(HealthCheckService.class);
        webService = registerMock(WebService.class);
        extension = factory.constructInstance(TxObservabilityApiExtension.class);
    }

    @Test
    void initialize() {
        extension.initialize(context);
        verify(healthCheckService).addReadinessProvider(any());
        verify(healthCheckService).addLivenessProvider(any());
        verifyNoMoreInteractions(healthCheckService);
    }

    @Test
    void initialize_allowInsecure_defaultContext() {
        when(context.getSetting(eq(TxObservabilityApiExtension.ALLOW_INSECURE_API_SETTING), any(String.class)))
                .thenReturn("true");

        extension.initialize(context);
        verify(webService).registerResource(eq(TxObservabilityApiExtension.OBSERVABILITY_CONTEXT_NAME), isA(TxObservabilityApiController.class));
    }

    private <T> T registerMock(Class<T> clazz) {
        var service = mock(clazz);
        context.registerService(clazz, service);
        return service;
    }
}
