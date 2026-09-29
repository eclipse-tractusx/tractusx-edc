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

import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.participantcontext.spi.identity.ParticipantIdentityResolver;
import org.eclipse.edc.protocol.dsp.http.spi.api.DspBaseWebhookAddress;
import org.eclipse.edc.protocol.spi.DataspaceProfileContext;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.tractusx.edc.protocol.cx.identifier.BpnExtractionFunction;
import org.eclipse.tractusx.edc.protocol.cx.identifier.CatenaxParticipantIdentityResolver;

import java.util.stream.Stream;

import static org.eclipse.edc.protocol.dsp.http.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp08Constants.V_08;

public class CxDataspaceProtocolExtension implements ServiceExtension {

    @Setting(description = "the BPN of the participant", key = "tractusx.edc.participant.bpn")
    private String bpn;
    
    @Inject
    private DataspaceProfileContextRegistry contextRegistry;
    @Inject
    private DspBaseWebhookAddress dspWebhookAddress;
    @Inject
    private SingleParticipantContextSupplier singleParticipantContextSupplier;
    @Inject
    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        Stream.of(
                new DataspaceProfileContext(DATASPACE_PROTOCOL_HTTP, V_08, () -> dspWebhookAddress.get(), new BpnExtractionFunction(monitor))
        ).forEach(contextRegistry::register);
    }

    @Provider
    public ParticipantIdentityResolver participantIdentityResolver() {
        return new CatenaxParticipantIdentityResolver(bpn, singleParticipantContextSupplier);
    }

}
