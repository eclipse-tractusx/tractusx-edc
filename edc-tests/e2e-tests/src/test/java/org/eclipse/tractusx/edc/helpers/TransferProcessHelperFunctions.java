/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.helpers;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

public class TransferProcessHelperFunctions {

    public static JsonObject createTransferRequest(String dataRequestId, String connectorAddress, String contractId, String assetId, boolean managedResources, JsonObject destination) {
        return Json.createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "TransferRequestDto")
                .add(ID, dataRequestId)
                .add(EDC_NAMESPACE + "connectorId", "connectorId")
                .add(EDC_NAMESPACE + "dataDestination", destination)
                .add(EDC_NAMESPACE + "protocol", DATASPACE_PROTOCOL_HTTP)
                .add(EDC_NAMESPACE + "assetId", assetId)
                .add(EDC_NAMESPACE + "contractId", contractId)
                .add(EDC_NAMESPACE + "connectorAddress", connectorAddress)
                .add(EDC_NAMESPACE + "managedResources", managedResources)

                .build();

    }


    public static JsonObject createProxyRequest() {
        return Json.createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "DataAddress")
                .add(EDC_NAMESPACE + "type", "HttpProxy")
                .build();

    }
}
