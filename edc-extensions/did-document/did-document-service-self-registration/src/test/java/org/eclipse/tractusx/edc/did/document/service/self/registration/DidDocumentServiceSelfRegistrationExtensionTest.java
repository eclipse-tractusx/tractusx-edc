/********************************************************************************
 * Copyright (c) 2025 SAP SE
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

package org.eclipse.tractusx.edc.did.document.service.self.registration;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.protocol.dsp.http.spi.api.DspBaseWebhookAddress;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.tractusx.edc.spi.did.document.service.DidDocumentServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.tractusx.edc.did.document.service.self.registration.DidDocumentServiceSelfRegistrationExtension.DATA_SERVICE_TYPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class DidDocumentServiceSelfRegistrationExtensionTest {

    private static final String SERVICE_ID = "did:web:example.com:connector1";
    private static final String DSP_URL = "https://protocol.edc.com/api/v1/dsp";

    private final Monitor monitor = mock(Monitor.class);
    private final DspBaseWebhookAddress dspBaseAddress = mock(DspBaseWebhookAddress.class);
    private final DidDocumentServiceClient didDocumentServiceClient = mock(DidDocumentServiceClient.class);

    @BeforeEach
    void setup(ServiceExtensionContext context) {
        context.registerService(Monitor.class, monitor);
        context.registerService(DspBaseWebhookAddress.class, dspBaseAddress);
        when(dspBaseAddress.get()).thenReturn(DSP_URL);
    }

    @Test
    void start_shouldSelfRegister_whenEnabledAndClientPresent(ServiceExtensionContext context, ObjectFactory objectFactory) {

        var settings = Map.of("tx.edc.did.service.self.registration.enabled", "true",
                "tx.edc.did.service.self.registration.id", SERVICE_ID);
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(settings));
        context.registerService(DidDocumentServiceClient.class, didDocumentServiceClient);
        when(didDocumentServiceClient.update(any(Service.class))).thenReturn(ServiceResult.success());
        when(didDocumentServiceClient.deleteById(anyString())).thenReturn(ServiceResult.success());

        var extension = objectFactory.constructInstance(DidDocumentServiceSelfRegistrationExtension.class);
        extension.start();

        verify(didDocumentServiceClient).update(argThat(service ->
                service.getId().equals(SERVICE_ID + "#" + DATA_SERVICE_TYPE) &&
                service.getType().equals(DATA_SERVICE_TYPE) &&
                service.getServiceEndpoint().equals(DSP_URL + "/.well-known/dspace-version")));
        verify(monitor).info("Self Registration of DID Document service successful");
        verify(monitor, never()).info("Did Document Service Client not available or not enabled, skipping self-registration");

        extension.shutdown();
        verify(didDocumentServiceClient).deleteById(SERVICE_ID + "#" + DATA_SERVICE_TYPE);
        verify(monitor).info("Successfully unregistered DID Document service");
    }

    @Test
    void start_selfRegister_whenEnabledAndClientReturnsFailure(ServiceExtensionContext context, ObjectFactory objectFactory) {

        var settings = Map.of("tx.edc.did.service.self.registration.enabled", "true",
                "tx.edc.did.service.self.registration.id", SERVICE_ID);
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(settings));
        context.registerService(DidDocumentServiceClient.class, didDocumentServiceClient);
        when(didDocumentServiceClient.update(any(Service.class))).thenReturn(ServiceResult.unexpected());
        when(didDocumentServiceClient.deleteById(anyString())).thenReturn(ServiceResult.unexpected());

        var extension = objectFactory.constructInstance(DidDocumentServiceSelfRegistrationExtension.class);
        extension.start();

        verify(didDocumentServiceClient).update(argThat(service ->
                service.getId().equals(SERVICE_ID + "#" + DATA_SERVICE_TYPE) &&
                service.getType().equals(DATA_SERVICE_TYPE) &&
                service.getServiceEndpoint().equals(DSP_URL + "/.well-known/dspace-version")));
        verify(monitor).severe(contains("Failed to self-register DID Document service"));
        verify(monitor, never()).info("Did Document Service Client not available or not enabled, skipping self-registration");

        extension.shutdown();
        verify(didDocumentServiceClient).deleteById(SERVICE_ID + "#" + DATA_SERVICE_TYPE);
        verify(monitor).severe(contains("Failed to unregister DID Document service"));
    }

    @Test
    void start_shouldNotSelfRegister_whenClientNotPresent(ObjectFactory objectFactory) {

        var extension = objectFactory.constructInstance(DidDocumentServiceSelfRegistrationExtension.class);
        extension.start();

        verify(didDocumentServiceClient, never()).update(any(Service.class));
        verify(monitor).info("Did Document Service Client not available or not enabled, skipping self-registration");

        extension.shutdown();
        verify(didDocumentServiceClient, never()).deleteById(anyString());
    }

    @Test
    void start_shouldNotSelfRegister_whenDisabledAndClientPresent(ServiceExtensionContext context, ObjectFactory objectFactory) {

        var settings = Map.of("tx.edc.did.service.self.registration.enabled", "false");
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(settings));
        context.registerService(DidDocumentServiceClient.class, didDocumentServiceClient);

        var extension = objectFactory.constructInstance(DidDocumentServiceSelfRegistrationExtension.class);
        extension.start();

        verify(didDocumentServiceClient, never()).update(any(Service.class));
        verify(monitor).info("Did Document Service Client not available or not enabled, skipping self-registration");

        extension.shutdown();
        verify(didDocumentServiceClient, never()).deleteById(anyString());
    }

    @Test
    void start_shouldThrowException_whenEnabledAndServiceIdMissing(ServiceExtensionContext context, ObjectFactory objectFactory) {

        var settings = Map.of("tx.edc.did.service.self.registration.enabled", "true");
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(settings));
        context.registerService(DidDocumentServiceClient.class, didDocumentServiceClient);

        var extension = objectFactory.constructInstance(DidDocumentServiceSelfRegistrationExtension.class);

        assertThatThrownBy(extension::start)
                .isInstanceOf(EdcException.class)
                .hasMessageContaining("Service ID for DID Document Service self-registration is not configured");

        verify(didDocumentServiceClient, never()).update(any(Service.class));
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {"   "})
    void start_shouldThrowException_whenEnabledAndServiceIdEmptyOrBlank(String serviceId, ServiceExtensionContext context, ObjectFactory objectFactory) {

        var settings = Map.of("tx.edc.did.service.self.registration.enabled", "true",
                "tx.edc.did.service.self.registration.id", serviceId);
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(settings));
        context.registerService(DidDocumentServiceClient.class, didDocumentServiceClient);

        var extension = objectFactory.constructInstance(DidDocumentServiceSelfRegistrationExtension.class);

        assertThatThrownBy(extension::start)
                .isInstanceOf(EdcException.class)
                .hasMessageContaining("Service ID for DID Document Service self-registration is not configured");

        verify(didDocumentServiceClient, never()).update(any(Service.class));
    }

    @Test
    void start_shouldThrowException_whenEnabledAndServiceIdInvalid(ServiceExtensionContext context, ObjectFactory objectFactory) {

        var settings = Map.of("tx.edc.did.service.self.registration.enabled", "true",
                "tx.edc.did.service.self.registration.id", "invalid uri");
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(settings));
        context.registerService(DidDocumentServiceClient.class, didDocumentServiceClient);

        var extension = objectFactory.constructInstance(DidDocumentServiceSelfRegistrationExtension.class);

        assertThatThrownBy(extension::start)
                .isInstanceOf(EdcException.class)
                .hasMessageContaining("does not contain a valid URI");

        verify(didDocumentServiceClient, never()).update(any(Service.class));
    }
}
