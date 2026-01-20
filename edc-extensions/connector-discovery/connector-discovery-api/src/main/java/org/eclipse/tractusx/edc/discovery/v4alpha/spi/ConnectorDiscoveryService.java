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
 * Service responsible for discovering published connectors of a Dataspace Participant and the version parameters for the
 * DSP (Dataspace Protocol) version to use for the corresponding connector. As in a dataspace multiple versions of DSP
 * might be active, the service determines the newest versions provided by both connectors and provides the parameters
 * to be used when initiating further DSP calls in the management api.
 * <p>
 * The detection of available connectors is provided via published connector endpoints in the DID document of the
 * corresponding Dataspace Participant.
 */
public interface ConnectorDiscoveryService {

    /**
     * Discovers version-specific connection parameters to be used for a given connector. This will always be the
     * parameters to initiate the use of the latest DSP version supported by both connectors.
     * <p>
     * This method attempts to determine the supported DSP protocol versions of a counterparty connector
     * and returns the appropriate connection parameters (protocol, counterPartyId, counterPartyAddress)
     * needed to establish communication with that connector.
     *
     * @param request The discovery request containing the counterparty id (e.g., the DID) and endpoint address.
     * @return A 'JsonObject' with the discovered connection parameters.
     * @throws RuntimeException A meaningful exception from 'org.eclipse.edc.web.spi.exception' to allow, e.g., a correct
     *                          selection of a status code for the request.
     */
    CompletableFuture<JsonObject> discoverVersionParams(ConnectorParamsDiscoveryRequest request);

    /**
     * Discovers version-specific connection parameters for all connectors published in the DID document of the
     * counterparty. For each connector this will always be the parameters to initiate the use of the latest DSP version
     * supported by both connectors.
     * <p>
     * This method attempts to determine the published connectors by downloading the DID document of the counterparty
     * and parse the service section for 'DataService' entries. For each endpoint found in this section and for all
     * additional connectors provided as request parameter in the 'knownConnectors' list, the 'discoverVersionParams'
     * service is executed to retrieve the supported DSP protocol versions of the corresponding counterparty connector.
     * For each found connector the appropriate connection parameters (protocol, counterPartyId, counterPartyAddress)
     * needed to establish communication with that connector are returned in an array.
     * <p>
     * The request will be processed in a way, that whenever the detection of the version information for one
     * connector fails, the corresponding connector will be ignored. The processing continues. An error is only thrown
     * when the result would be empty.
     *
     * @param request The discovery request containing the counterparty id (e.g., the DID) and a list of additional
     *                known connector endpoints
     * @return A 'JsonArray' build from one 'JsonObjects' with the discovered connection parameters for each found connector.
     * @throws RuntimeException A meaningful exception from 'org.eclipse.edc.web.spi.exception' to allow, e.g., a correct
     *                          selection of a status code for the request.
     */
    CompletableFuture<JsonArray> discoverConnectors(ConnectorDiscoveryRequest request);
}
