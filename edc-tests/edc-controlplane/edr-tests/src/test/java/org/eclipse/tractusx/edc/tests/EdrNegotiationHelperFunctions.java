/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.tests;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.tractusx.edc.api.edr.dto.NegotiateEdrRequestDto;

import java.util.Set;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.mockito.Mockito.mock;

public class EdrNegotiationHelperFunctions {

    private static final JsonLd JSON_LD = new TitaniumJsonLd(mock(Monitor.class));

    public static JsonObject createEdrNegotiationRequest(String connectorAddress, String providerId, String offerId, String assetId, JsonObject policy, JsonArray callbacks) {
        return Json.createObjectBuilder()
                .add(TYPE, NegotiateEdrRequestDto.EDR_REQUEST_DTO_TYPE)
                .add(EDC_NAMESPACE + "counterPartyId", providerId)
                .add(EDC_NAMESPACE + "providerId", providerId)
                .add(EDC_NAMESPACE + "counterPartyAddress", connectorAddress)
                .add(EDC_NAMESPACE + "protocol", "dataspace-protocol-http")
                .add(EDC_NAMESPACE + "offer", Json.createObjectBuilder()
                        .add(EDC_NAMESPACE + "offerId", offerId)
                        .add(EDC_NAMESPACE + "assetId", assetId)
                        .add(EDC_NAMESPACE + "policy", JSON_LD.compact(policy).getContent())
                )
                .add(EDC_NAMESPACE + "callbackAddresses", callbacks)
                .build();
    }

    public static JsonObject createCallback(String url, boolean transactional, Set<String> events) {
        return Json.createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "CallbackAddress")
                .add(EDC_NAMESPACE + "transactional", transactional)
                .add(EDC_NAMESPACE + "uri", url)
                .add(EDC_NAMESPACE + "events", events
                        .stream()
                        .collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add)
                        .build())
                .build();
    }
}
