/*
 * Copyright (c) 2025 Think-it GmbH
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

package org.eclipse.tractusx.edc.protocol.cx.identifier;

import org.eclipse.edc.participantcontext.spi.service.ParticipantContextSupplier;
import org.eclipse.tractusx.edc.protocol.core.DefaultParticipantIdentityResolver;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.protocol.dsp.http.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2024Constants.DATASPACE_PROTOCOL_HTTP_V_2024_1;

public class CatenaxParticipantIdentityResolver extends DefaultParticipantIdentityResolver {
    private final String bpn;

    public CatenaxParticipantIdentityResolver(String bpn, ParticipantContextSupplier participantContextSupplier) {
        super(participantContextSupplier);
        this.bpn = bpn;
    }

    @Nullable
    @Override
    public String getParticipantId(String participantContextId, String protocol) {
        if (DATASPACE_PROTOCOL_HTTP.equals(protocol) || DATASPACE_PROTOCOL_HTTP_V_2024_1.equals(protocol)) {
            return bpn;
        }
        return super.getParticipantId(participantContextId, protocol);
    }
}
