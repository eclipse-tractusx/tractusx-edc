/********************************************************************************
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

package org.eclipse.tractusx.edc.tests.helpers;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import static jakarta.json.Json.createObjectBuilder;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

public class TransferProcessHelperFunctions {

    public static JsonObject createTransferRequest(String dataRequestId, String counterPartyAddress, String contractId, String assetId, boolean managedResources, JsonObject destination) {
        return Json.createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "TransferRequestDto")
                .add(ID, dataRequestId)
                .add(EDC_NAMESPACE + "connectorId", "connectorId")
                .add(EDC_NAMESPACE + "dataDestination", destination)
                .add(EDC_NAMESPACE + "protocol", "dataspace-protocol-http")
                .add(EDC_NAMESPACE + "assetId", assetId)
                .add(EDC_NAMESPACE + "contractId", contractId)
                .add(EDC_NAMESPACE + "counterPartyAddress", counterPartyAddress)
                .add(EDC_NAMESPACE + "managedResources", managedResources)
                .build();

    }

    public static JsonObject createProxyRequest() {
        return Json.createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "DataAddress")
                .add(EDC_NAMESPACE + "type", "HttpProxy")
                .build();

    }

    public static JsonObject httpDataAddress(String baseUrl) {
        return createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "DataAddress")
                .add(EDC_NAMESPACE + "type", "HttpData")
                .add(EDC_NAMESPACE + "properties", createObjectBuilder()
                        .add(EDC_NAMESPACE + "baseUrl", baseUrl)
                        .build())
                .build();
    }
}
