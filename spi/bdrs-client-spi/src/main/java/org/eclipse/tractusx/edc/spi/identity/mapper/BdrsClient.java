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

import java.util.UUID;

/**
 * Interface for resolving BPNs to DIDs.
 * Every resolution is scoped to a specific tenant via a participantContextId,
 * so that different participants can have independent cache partitions.
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

}
