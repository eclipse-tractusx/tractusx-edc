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

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_NAMESPACE;

/**
 * Request object for discovering connector parameters for a specific counterparty.
 * Contains the necessary information to identify and locate a counterparty connector
 * for DSP (Dataspace Protocol) communication.
 *
 * @param identifier the Business Partner Number Legal (BPNL) counterPartyId of the counterparty
 * @param counterPartyAddress the base address/URL of the counterparty's DSP endpoint
 */
public record ConnectorParamsDiscoveryRequest(String identifier, String counterPartyAddress) {

    public static final String TYPE = TX_NAMESPACE +  "ConnectorParamsDiscoveryRequest";
    public static final String DISCOVERY_PARAMS_REQUEST_IDENTIFIER_ATTRIBUTE_LEGACY = TX_NAMESPACE + "bpnl";
    public static final String DISCOVERY_PARAMS_REQUEST_IDENTIFIER_ATTRIBUTE = EDC_NAMESPACE + "counterPartyId";
    public static final String DISCOVERY_PARAMS_REQUEST_COUNTER_PARTY_ADDRESS_ATTRIBUTE = EDC_NAMESPACE + "counterPartyAddress";
}
