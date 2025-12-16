/********************************************************************************
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

package org.eclipse.tractusx.edc.iam.dcp;

import org.eclipse.edc.iam.decentralizedclaims.lib.DefaultPresentationRequestService;
import org.eclipse.edc.iam.decentralizedclaims.spi.CredentialServiceClient;
import org.eclipse.edc.iam.decentralizedclaims.spi.CredentialServiceUrlResolver;
import org.eclipse.edc.iam.decentralizedclaims.spi.SecureTokenService;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentationContainer;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.tractusx.edc.spi.dcp.VerifiablePresentationCache;

import java.util.List;

import static java.lang.String.format;

public class CachePresentationRequestService extends DefaultPresentationRequestService {

    private final VerifiablePresentationCache cache;
    private final Monitor monitor;

    public CachePresentationRequestService(SecureTokenService secureTokenService,
                                           CredentialServiceUrlResolver credentialServiceUrlResolver,
                                           CredentialServiceClient credentialServiceClient,
                                           VerifiablePresentationCache cache, Monitor monitor) {
        super(secureTokenService, credentialServiceUrlResolver, credentialServiceClient);
        this.cache = cache;
        this.monitor = monitor;
    }

    @Override
    public Result<List<VerifiablePresentationContainer>> requestPresentation(String participantContextId, String ownDid,
                                                                             String counterPartyDid, String counterPartyToken,
                                                                             List<String> scopes) {
        var cacheResult = cache.query(participantContextId, counterPartyDid, scopes);
        if (cacheResult.succeeded()) {
            return Result.success(cacheResult.getContent());
        }

        var vpResult = super.requestPresentation(participantContextId, ownDid, counterPartyDid, counterPartyToken, scopes);

        if (vpResult.succeeded()) {
            var storeResult = cache.store(participantContextId, counterPartyDid, scopes, vpResult.getContent());
            if (storeResult.failed()) {
                monitor.warning(format("Failed to cache Verifiable Presentation for %s: %s", counterPartyDid, storeResult.getFailureDetail()));
            }
        }

        return vpResult;
    }
}
