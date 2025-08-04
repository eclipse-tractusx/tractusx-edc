/*
 * Copyright (c) 2025 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.discovery.models;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_NAMESPACE;

public record ConnectorParamsDiscoveryRequest(String bpnl, String counterPartyAddress) {

    public static final String TYPE = TX_NAMESPACE +  "ConnectorParamsDiscoveryRequest";
    public static final String DISCOVERY_PARAMS_REQUEST_BPNL_ATTRIBUTE = TX_NAMESPACE + "bpnl";
    public static final String DISCOVERY_PARAMS_REQUEST_COUNTER_PARTY_ADDRESS_ATTRIBUTE = EDC_NAMESPACE + "counterPartyAddress";

    public static final String EXAMPLE = """
            {
                "@context": {
                    "tx": "https://w3id.org/tractusx/v0.0.1/ns/",
                    "edc": "https://w3id.org/edc/v0.0.1/ns/",
                },
                "tx:bpnl": "BPNL1234567890",
                "edc:counterPartyAddress": "https://provider.domain.com/api/dsp"
            }
            """;
}
