/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.common.tokenrefresh;

import org.eclipse.edc.edr.spi.store.EndpointDataReferenceCache;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.decentralizedclaims.spi.SecureTokenService;
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.tractusx.edc.spi.tokenrefresh.common.TokenRefreshHandler;

import static org.eclipse.tractusx.edc.common.tokenrefresh.TokenRefreshHandlerExtension.NAME;

@Extension(value = NAME)
public class TokenRefreshHandlerExtension implements ServiceExtension {
    public static final String NAME = "Token Refresh Handler Extension";

    @Inject
    private Monitor monitor;
    @Inject
    private EndpointDataReferenceCache edrStore;
    @Inject
    private EdcHttpClient httpClient;
    @Inject
    private SecureTokenService secureTokenService;
    @Inject
    private TypeManager typeManager;
    @Inject
    private SingleParticipantContextSupplier participantContextSupplier;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public TokenRefreshHandler createTokenRefreshHander(ServiceExtensionContext context) {
        return new TokenRefreshHandlerImpl(edrStore, httpClient, getOwnDid(), context.getMonitor(),
                secureTokenService, typeManager.getMapper(), participantContextSupplier);
    }

    private String getOwnDid() {
        return participantContextSupplier.get().map(ParticipantContext::getIdentity).onFailure(f -> {
            var message = "This connector is not configured properly, cannot continue. Error is: %s".formatted(f.getFailureDetail());
            monitor.withPrefix(getClass().getSimpleName()).severe(message);
            throw new EdcException(message);
        }).getContent();
    }
}
