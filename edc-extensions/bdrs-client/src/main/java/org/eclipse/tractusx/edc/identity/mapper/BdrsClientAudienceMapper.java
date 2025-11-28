/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 * Copyright (c) 2025 Cofinity-X GmbH
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

package org.eclipse.tractusx.edc.identity.mapper;

import org.eclipse.edc.spi.iam.AudienceResolver;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.message.ProtocolRemoteMessage;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;

import java.util.Optional;

import static org.eclipse.tractusx.edc.spi.identity.mapper.BdrsConstants.DID_PREFIX;

/**
 * Extracts the audience from a {@link RemoteMessage} using {@link RemoteMessage#getCounterPartyId()}.
 * If the counter-party id is a DID, returns it as-is. If it is a BPN, calls {@link BdrsClient#resolveDid(String)}
 * to resolve the corresponding DID.
 */
class BdrsClientAudienceMapper implements AudienceResolver {
    
    private final BdrsClient client;

    BdrsClientAudienceMapper(BdrsClient client) {
        this.client = client;
    }

    @Override
    public Result<String> resolve(ProtocolRemoteMessage remoteMessage) {
        try {
            var counterPartyId = remoteMessage.getCounterPartyId();
            if (counterPartyId.startsWith(DID_PREFIX)) {
                return Result.success(counterPartyId);
            }
            
            var resolve = client.resolveDid(counterPartyId);
            return Result.from(Optional.ofNullable(resolve));
        } catch (Exception e) {
            return Result.failure("Failure in DID resolution: " + e.getMessage());
        }
    }

}
