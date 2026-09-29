/*
 * Copyright (c) 2025 Cofinity-X GmbH
 * Copyright (c) 2026 SAP SE
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
 */

package org.eclipse.tractusx.edc.protocol.cx;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.protocol.dsp.http.spi.api.DspBaseWebhookAddress;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.tractusx.edc.protocol.cx.identifier.BpnExtractionFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.eclipse.edc.protocol.dsp.http.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp08Constants.V_08;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class CxDataspaceProtocolExtensionTest {
    
    private final String webhook = "https://webhook";
    private final String bpn = "bpn";

    private final DataspaceProfileContextRegistry dataspaceProfileContextRegistry = mock();
    private final DspBaseWebhookAddress dspBaseWebhookAddress = mock();
    
    @BeforeEach
    void setup(ServiceExtensionContext context) {
        context.registerService(DataspaceProfileContextRegistry.class, dataspaceProfileContextRegistry);
        context.registerService(DspBaseWebhookAddress.class, dspBaseWebhookAddress);
        
        when(dspBaseWebhookAddress.get()).thenReturn(webhook);
    }
    
    @Test
    void initialize_shouldRegisterProfileContexts(ObjectFactory factory, ServiceExtensionContext context) {
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of("tractusx.edc.participant.bpn", bpn)));
        
        factory.constructInstance(CxDataspaceProtocolExtension.class).initialize(context);
        
        verify(dataspaceProfileContextRegistry).register(argThat(
                dataspaceProfileContext -> dataspaceProfileContext.name().equals(DATASPACE_PROTOCOL_HTTP) &&
                        dataspaceProfileContext.protocolVersion().equals(V_08) &&
                        dataspaceProfileContext.webhook().url().equals(webhook) &&
                        dataspaceProfileContext.idExtractionFunction() instanceof BpnExtractionFunction));
    }
}
