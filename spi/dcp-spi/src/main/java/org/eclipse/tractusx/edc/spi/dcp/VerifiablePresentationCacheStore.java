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

import org.eclipse.edc.spi.result.StoreResult;

import java.util.List;

/**
 * Store for the {@link VerifiablePresentationCache} to decouple the persistence layer from common
 * cache behaviour, to allow e.g. utilizing external cache solutions. The ID used for storing
 * entries needs to be a compound ID comprising participant context ID, participant DID and scopes
 * from the {@link VerifiablePresentationCacheEntry}.
 */
public interface VerifiablePresentationCacheStore {

    /**
     * Stores a new entry in the cache. If an entry already exists for the given participant context
     * ID, participant DID and scopes, the entry is overridden.
     *
     * @param entry the entry to cache
     * @return successful result, if the entry was stored; failed result otherwise
     */
    StoreResult<Void> store(VerifiablePresentationCacheEntry entry);

    /**
     * Queries the store for an existing entry.
     *
     * @param participantContextId ID of the participant context
     * @param counterPartyDid DID of the participant
     * @param scopes scopes
     * @return successful result containing the found entry, if present; failed result otherwise
     */
    StoreResult<VerifiablePresentationCacheEntry> query(String participantContextId, String counterPartyDid, List<String> scopes);

    /**
     * Removes a single entry for the given participant context ID, participant DID and scopes.
     *
     * @param participantContextId ID of the participant context
     * @param counterPartyDid DID of the participant
     * @param scopes scopes
     * @return successful result, if the entry was deleted; failed result otherwise
     */
    StoreResult<Void> remove(String participantContextId, String counterPartyDid, List<String> scopes);

    /**
     * Removes all entries for a given participant context ID and participant DID.
     *
     * @param participantContextId ID of the participant context
     * @param counterPartyDid DID of the participant
     * @return successful result, if all entries were deleted; failed result otherwise
     */
    StoreResult<Void> remove(String participantContextId, String counterPartyDid);

}
