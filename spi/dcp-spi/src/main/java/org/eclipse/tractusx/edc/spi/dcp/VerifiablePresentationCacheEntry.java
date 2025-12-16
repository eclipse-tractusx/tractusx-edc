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

package org.eclipse.tractusx.edc.spi.dcp;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentationContainer;

import java.time.Instant;
import java.util.List;

/**
 * Model class for an entry in the {@link VerifiablePresentationCacheStore}. Comprises all
 * information to be cached including an instant when the cache entry was created.
 */
public class VerifiablePresentationCacheEntry {

    private final String participantContextId;
    private final String counterPartyDid;
    private final List<String> scopes;
    private final List<VerifiablePresentationContainer> presentations;
    private final Instant cachedAt;

    public VerifiablePresentationCacheEntry(String participantContextId, String counterPartyDid, List<String> scopes,
                                            List<VerifiablePresentationContainer> presentations, Instant cachedAt) {
        this.participantContextId = participantContextId;
        this.counterPartyDid = counterPartyDid;
        this.scopes = scopes;
        this.presentations = presentations;
        this.cachedAt = cachedAt;
    }

    public String getParticipantContextId() {
        return participantContextId;
    }

    public String getCounterPartyDid() {
        return counterPartyDid;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public Instant getCachedAt() {
        return cachedAt;
    }

    public List<VerifiablePresentationContainer> getPresentations() {
        return presentations;
    }

}
