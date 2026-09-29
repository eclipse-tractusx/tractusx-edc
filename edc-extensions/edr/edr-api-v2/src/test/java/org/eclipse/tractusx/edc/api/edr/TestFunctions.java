/*
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
 */

package org.eclipse.tractusx.edc.api.edr;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

public class TestFunctions {

    public static JsonObject negotiationRequest() {
        return Json.createObjectBuilder()
                .add(TYPE, ContractRequest.CONTRACT_REQUEST_TYPE)
                .add(EDC_NAMESPACE + "connectorId", "test")
                .add(EDC_NAMESPACE + "providerId", "test")
                .add(EDC_NAMESPACE + "connectorAddress", "test")
                .add(EDC_NAMESPACE + "protocol", "dataspace-protocol-http")
                .add(EDC_NAMESPACE + "offer", Json.createObjectBuilder()
                        .add(EDC_NAMESPACE + "offerId", "offerId")
                        .add(EDC_NAMESPACE + "assetId", "assetId")
                        .add(EDC_NAMESPACE + "policy", Json.createObjectBuilder().build())
                )
                .build();
    }

    public static ContractNegotiation createContractNegotiation() {
        return ContractNegotiation.Builder.newInstance()
                .id("id")
                .counterPartyAddress("http://test")
                .counterPartyId("provider")
                .protocol("protocol")
                .build();
    }
}
