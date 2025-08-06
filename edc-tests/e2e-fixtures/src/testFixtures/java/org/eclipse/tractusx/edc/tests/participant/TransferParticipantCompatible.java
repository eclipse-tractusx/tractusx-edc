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

package org.eclipse.tractusx.edc.tests.participant;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.test.system.utils.Participant;

import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createObjectBuilder;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Extension of {@link TransferParticipantCompatible} with Transfer specific configuration
 */
public class TransferParticipantCompatible extends TransferParticipant {

    @Override
    public String initContractNegotiation(Participant provider, JsonObject policy) {
        var requestBody = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add(VOCAB, EDC_NAMESPACE))
                .add(TYPE, "ContractRequest")
                .add("counterPartyAddress", provider.getProtocolUrl())
                .add("protocol", protocol)
                .add("policy", jsonLd.compact(policy).getContent())
                .build();

        return baseManagementRequest()
                .contentType(JSON)
                .body(requestBody)
                .when()
                .post("/v3/contractnegotiations")
                .then()
                .log().ifError()
                .statusCode(200)
                .extract().body().jsonPath().getString(ID);
    }

    @Override
    public String initiateTransfer(Participant provider, String contractAgreementId, JsonObject privateProperties, JsonObject destination, String transferType, JsonArray callbacks) {
        var requestBodyBuilder = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add(VOCAB, EDC_NAMESPACE))
                .add(TYPE, "TransferRequest")
                .add("protocol", protocol)
                .add("contractId", contractAgreementId)
                .add("connectorId", provider.getId())
                .add("counterPartyAddress", provider.getProtocolUrl());

        if (privateProperties != null) {
            requestBodyBuilder.add("privateProperties", privateProperties);
        }

        if (destination != null) {
            requestBodyBuilder.add("dataDestination", destination);
        }

        if (transferType != null) {
            requestBodyBuilder.add("transferType", transferType);
        }

        if (callbacks != null) {
            requestBodyBuilder.add("callbackAddresses", callbacks);
        }

        var requestBody = requestBodyBuilder.build();

        return baseManagementRequest()
                .contentType(JSON)
                .body(requestBody)
                .when()
                .post("/v3/transferprocesses")
                .then()
                .log().ifError()
                .statusCode(200)
                .extract().body().jsonPath().getString(ID);
    }
}
