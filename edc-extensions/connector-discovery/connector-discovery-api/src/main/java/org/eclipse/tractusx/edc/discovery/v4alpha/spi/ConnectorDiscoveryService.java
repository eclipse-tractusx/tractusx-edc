/*
 * Copyright (c) 2025 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 * Copyright (c) 2026 Cofinity-X GmbH
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
 */

package org.eclipse.tractusx.edc.discovery.v4alpha.spi;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;

import java.util.concurrent.CompletableFuture;

/**
 * Service responsible for discovering connector parameters for different DSP (Dataspace Protocol) versions.
 * This service enables clients to determine the appropriate connection parameters needed to communicate
 * with EDC connectors based on their supported protocol versions.
 */
public interface ConnectorDiscoveryService {

    /**
     * Discovers version-specific connection parameters for a given connector.
     *
     * This method attempts to determine the supported DSP protocol versions of a counterparty connector
     * and returns the appropriate connection parameters (protocol, counterPartyId, counterPartyAddress)
     * needed to establish communication with that connector.
     *
     * The discovery process should prioritize the latest DSP version.
     *
     * @param request the discovery request containing the BPNL and counterparty address
     * @return a ServiceResult containing a JsonObject with the discovered connection parameters,
     *         or a failure result if discovery was unsuccessful
     */
    CompletableFuture<JsonObject> discoverVersionParams(ConnectorParamsDiscoveryRequest request);

    CompletableFuture<JsonArray> discoverConnectors(ConnectorDiscoveryRequest request);
}
