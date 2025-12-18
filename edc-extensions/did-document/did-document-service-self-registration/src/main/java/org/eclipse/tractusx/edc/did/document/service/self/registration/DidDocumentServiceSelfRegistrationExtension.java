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

import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.tractusx.edc.spi.did.document.service.DidDocumentServiceClient;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class DidDocumentServiceSelfRegistrationExtension implements ServiceExtension {

    public static final String DATA_SERVICE_TYPE = "DataService";

    @Inject
    private Monitor monitor;

    @Inject(required = false)
    private DidDocumentServiceClient didDocumentServiceClient;

    @Setting(key = "edc.dsp.callback.address", required = false, description = "DSP API Url")
    private String dspApiUrl;

    @Setting(key = "tx.edc.did.service.self.registration.enabled", defaultValue = "false", description = "Enable self-registration of the DID Document Service")
    private boolean selfRegistrationEnabled;

    @Setting(key = "tx.edc.did.service.self.registration.id", required = false, description = "The Id to use for service self-registration")
    private String serviceId;

    @Override
    public void start() {
        Optional.ofNullable(didDocumentServiceClient)
                .filter(client -> selfRegistrationEnabled)
                .ifPresentOrElse(this::selfRegisterDidDocumentService,
                        () -> monitor.info("Did Document Service Client not available or not enabled, skipping self-registration"));
    }

    @Override
    public void shutdown() {
        Optional.ofNullable(didDocumentServiceClient)
                .filter(client -> selfRegistrationEnabled)
                .ifPresent(this::selfUnregisterDidDocumentService);
    }

    private void selfRegisterDidDocumentService(@NotNull DidDocumentServiceClient client) {

        var wellKnownUrl = "%s/.well-known/dspace-version".formatted(dspApiUrl);
        var serviceIdWithType = "%s#%s".formatted(serviceId, DATA_SERVICE_TYPE);
        var service = new Service(serviceIdWithType, DATA_SERVICE_TYPE, wellKnownUrl);
        client.update(service)
                .onFailure(failure -> monitor.severe("Failed to self-register DID Document service: %s, reason: %s".formatted(failure.getFailureDetail(), failure.getReason())))
                .onSuccess(result -> monitor.info("Self Registration of DID Document service successful"));
    }

    private void selfUnregisterDidDocumentService(@NotNull DidDocumentServiceClient client) {

        var serviceIdWithType = "%s#%s".formatted(serviceId, DATA_SERVICE_TYPE);
        client.deleteById(serviceIdWithType)
                .onFailure(failure -> monitor.severe("Failed to unregister DID Document service: %s, reason: %s".formatted(failure.getFailureDetail(), failure.getReason())))
                .onSuccess(result -> monitor.info("Successfully unregistered DID Document service"));
    }
}
