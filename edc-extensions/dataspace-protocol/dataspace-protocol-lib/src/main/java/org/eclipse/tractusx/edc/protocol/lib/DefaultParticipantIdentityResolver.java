/********************************************************************************
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
 ********************************************************************************/

package org.eclipse.tractusx.edc.protocol.lib;

import org.eclipse.edc.participantcontext.spi.identity.ParticipantIdentityResolver;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextSupplier;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.EdcException;
import org.jetbrains.annotations.Nullable;

public class DefaultParticipantIdentityResolver implements ParticipantIdentityResolver {
    private final ParticipantContextSupplier participantContextSupplier;

    public DefaultParticipantIdentityResolver(ParticipantContextSupplier participantContextSupplier) {
        this.participantContextSupplier = participantContextSupplier;
    }

    @Nullable
    @Override
    public String getParticipantId(String participantContextId, String protocol) {
        return participantContextSupplier.get().map(ParticipantContext::getIdentity)
                .orElseThrow(f -> new EdcException("Cannot get the participant context: " + f.getFailureDetail()));
    }
}
