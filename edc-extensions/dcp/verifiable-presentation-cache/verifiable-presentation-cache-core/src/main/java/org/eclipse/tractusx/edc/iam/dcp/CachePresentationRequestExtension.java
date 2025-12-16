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
import org.eclipse.edc.iam.decentralizedclaims.spi.PresentationRequestService;
import org.eclipse.edc.iam.decentralizedclaims.spi.SecureTokenService;
import org.eclipse.edc.iam.verifiablecredentials.VerifiableCredentialValidationServiceImpl;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.RevocationServiceRegistry;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.PresentationVerifier;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.TrustedIssuerRegistry;
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.tractusx.edc.iam.dcp.cache.VerifiablePresentationCacheImpl;
import org.eclipse.tractusx.edc.spi.dcp.VerifiablePresentationCacheStore;

import java.time.Clock;

import static org.eclipse.tractusx.edc.iam.dcp.cache.VerifiablePresentationCacheImpl.DEFAULT_VP_CACHE_VALIDITY_SECONDS;

public class CachePresentationRequestExtension implements ServiceExtension {

    @Setting(key = "tx.edc.dcp.cache.enabled", defaultValue = "true", description = "Defines whether the Verifiable Presentation Cache is enabled.")
    private boolean cacheEnabled;

    @Setting(key = "tx.edc.dcp.cache.validity.seconds", defaultValue = DEFAULT_VP_CACHE_VALIDITY_SECONDS + "", min = 1,
            description = "Validity period of the Verifiable Presentation Cache in seconds.")
    private long cacheValidity;

    @Inject
    private VerifiablePresentationCacheStore store;
    @Inject
    private Clock clock;
    @Inject
    private PresentationVerifier presentationVerifier;
    @Inject
    private TrustedIssuerRegistry trustedIssuerRegistry;
    @Inject
    private RevocationServiceRegistry revocationServiceRegistry;
    @Inject
    private SecureTokenService secureTokenService;
    @Inject
    private CredentialServiceUrlResolver credentialServiceUrlResolver;
    @Inject
    private CredentialServiceClient credentialServiceClient;
    @Inject
    private TypeManager typeManager;
    @Inject
    private SingleParticipantContextSupplier singleParticipantContextSupplier;
    @Inject
    private Monitor monitor;

    @Provider
    public PresentationRequestService cachePresentationRequestService() {
        if (!cacheEnabled) {
            return new DefaultPresentationRequestService(secureTokenService, credentialServiceUrlResolver, credentialServiceClient);
        }

        var validationService = new VerifiableCredentialValidationServiceImpl(presentationVerifier, trustedIssuerRegistry, revocationServiceRegistry, clock, typeManager.getMapper());
        var cache = new VerifiablePresentationCacheImpl(cacheValidity, clock, store, validationService, this::resolveOwnDid);

        return new CachePresentationRequestService(secureTokenService, credentialServiceUrlResolver, credentialServiceClient, cache, monitor);
    }

    private String resolveOwnDid(String participantContextId) {
        return singleParticipantContextSupplier.get().map(ParticipantContext::getIdentity)
                .orElseThrow(f -> new EdcException("Cannot get the participant context: " + f.getFailureDetail()));
    }
}
