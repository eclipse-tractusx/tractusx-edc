/********************************************************************************
 * Copyright (c) 2025 Cofinity-X GmbH
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

package org.eclipse.tractusx.edc.spi.identity.mapper;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;

import java.util.Map;
import java.util.UUID;

/**
 * Interface for resolving BPNs to DIDs
 * a participantId parameter so that the resolution is scoped to a specific tenant. If the
 * BDRS server returns different mappings depending on the caller's credentials, the
 * resolution must be participant-aware.
 */
@ExtensionPoint
public interface BdrsClient {

    /**
     * Resolve the input BPN to a DID context aware
     *
     * @param participantContextId @type UUID the parameter needed for multi-tenant context mapping
     * @param bpn The participantID (BPN)
     * @return The resolved DID if found, null otherwise
     */
    String resolveDid(UUID participantContextId, String bpn);

    /**
     * Resolve the input DID to a BPN context aware
     *
     * @param participantContextId @type UUID the parameter needed for multi-tenant context mapping
     * @param did The participantID (DID)
     * @return The resolved BPN if found, null otherwise
     */
    String resolveBpn(UUID participantContextId, String did);


    /**
     * Set cached entry context aware
     *
     * @param participantContextId @type UUID the parameter needed for multi-tenant context mapping
     * @param bpn The participantID (BPN)
     * @param did The participantID (DID)
     * @return The cache HashMap if found
     */
    Boolean setCacheEntry(UUID participantContextId, String bpn, String did);

    /**
     * Get cached entry context aware
     *
     * @param participantContextId @type UUID the parameter needed for multi-tenant context mapping
     * @param bpn The participantID (BPN)
     * @return The cache HashMap if found
     */
    String getCacheEntry(UUID participantContextId, String bpn);

    /**
     * Purge cache entry context aware
     *
     * @param participantContextId @type UUID the parameter needed for multi-tenant context mapping
     * @return True if purged, False (Exception) otherwise
     */
    Boolean purgeCacheEntry(UUID participantContextId, String bpn);

    /**
     * Get cache context aware
     *
     * @param participantContextId @type UUID the parameter needed for multi-tenant context mapping
     * @return True if updated, False (Exception) otherwise
     */
    Map<String, String> getCache(UUID participantContextId);

    /**
     * Set cache context aware
     *
     * @param participantContextId @type UUID the parameter needed for multi-tenant context mapping
     * @param cache the cache to be introduced at the participantContextId entry.
     * @return True if updated, False (Exception) otherwise
     */
    Boolean setCache(UUID participantContextId, Map<String, String> cache);

    /**
     * Purge cache context aware
     *
     * @param participantContextId @type UUID the parameter needed for multi-tenant context mapping
     * @return True if purged, False (Exception) otherwise
     */
    Boolean purgeCache(UUID participantContextId);

}
