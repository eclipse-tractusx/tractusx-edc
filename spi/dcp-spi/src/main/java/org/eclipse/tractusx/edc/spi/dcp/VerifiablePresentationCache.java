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
import org.eclipse.edc.spi.result.StoreResult;

import java.util.List;

/**
 * A cache for Verifiable Presentations (VP), so that they can be reused during the DCP presentation
 * flow after initial request. As a VP is always requested for a specific participant and a
 * specific set of scopes, both of these need to be used for caching. Additionally, the ID of the
 * participant context is passed through to the cache for cases where multiple participant contexts
 * may be used within a connector.
 */
public interface VerifiablePresentationCache {

    /**
     * Stores a new entry in the cache.
     *
     * @param participantContextId ID of the participant context
     * @param counterPartyDid DID of the participant the VPs were requested for
     * @param scopes scopes used for the presentation request
     * @param presentations the VPs to cache
     * @return successful result, if the VPs were stored in the cache; failed result otherwise
     */
    StoreResult<Void> store(String participantContextId, String counterPartyDid, List<String> scopes, List<VerifiablePresentationContainer> presentations);

    /**
     * Queries the cache for an existing entry for a given participant context, participant and set
     * of scopes.
     *
     * @param participantContextId ID of the participant context
     * @param counterPartyDid DID of the participant to request the VPs for
     * @param scopes scopes to request
     * @return successful result containing the cached entry, if present; failed result otherwise
     */
    StoreResult<List<VerifiablePresentationContainer>> query(String participantContextId, String counterPartyDid, List<String> scopes);

    /**
     * Removes all cached entries for a given participant context and participant.
     *
     * @param participantContextId ID of the participant context
     * @param counterPartyDid DID of the participant for which the cached entries should be deleted
     * @return successful result, if all entries were deleted; failed result otherwise
     */
    StoreResult<Void> remove(String participantContextId, String counterPartyDid);
}
