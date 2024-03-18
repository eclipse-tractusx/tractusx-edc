/*
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft
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

package org.eclipse.tractusx.edc.api.edr.v2;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractOfferDescription;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.offer.ContractOffer;
import org.eclipse.tractusx.edc.api.edr.v2.dto.NegotiateEdrRequestDto;
import org.eclipse.tractusx.edc.edr.spi.types.NegotiateEdrRequest;

import java.util.UUID;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

public class TestFunctions {

    public static ContractOfferDescription createOffer(String offerId, String assetId) {
        return ContractOfferDescription.Builder.newInstance()
                .offerId(offerId)
                .assetId(assetId)
                .policy(Policy.Builder.newInstance().build())
                .build();
    }

    public static ContractOfferDescription createOffer(Policy policy) {
        return ContractOfferDescription.Builder.newInstance()
                .offerId(UUID.randomUUID().toString())
                .assetId(UUID.randomUUID().toString())
                .policy(policy)
                .build();
    }

    public static ContractOfferDescription createOffer(String offerId) {
        return createOffer(offerId, UUID.randomUUID().toString());
    }

    public static ContractOfferDescription createOffer() {
        return createOffer(UUID.randomUUID().toString(), UUID.randomUUID().toString());
    }

    public static JsonObject negotiationRequest() {
        return Json.createObjectBuilder()
                .add(TYPE, NegotiateEdrRequestDto.EDR_REQUEST_DTO_TYPE)
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

    public static NegotiateEdrRequest openRequest() {
        return NegotiateEdrRequest.Builder.newInstance()
                .connectorAddress("test")
                .connectorId("id")
                .protocol("test-protocol")
                .offer(ContractOffer.Builder.newInstance()
                        .id("offerId")
                        .assetId("assetId")
                        .policy(Policy.Builder.newInstance().build()).build())
                .build();
    }
}
