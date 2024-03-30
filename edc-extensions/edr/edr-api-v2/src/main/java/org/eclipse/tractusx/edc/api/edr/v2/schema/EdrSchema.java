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

package org.eclipse.tractusx.edc.api.edr.v2.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import org.eclipse.edc.connector.api.management.configuration.ManagementApiSchema;
import org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.ContractNegotiationApi;
import org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.eclipse.tractusx.edc.api.edr.v2.dto.NegotiateEdrRequestDto;

import java.util.List;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.tractusx.edc.api.edr.v2.schema.EdrSchema.EndpointDataReferenceEntrySchema.ENDPOINT_DATA_REFERENCE_ENTRY_EXAMPLE;
import static org.eclipse.tractusx.edc.api.edr.v2.schema.EdrSchema.NegotiateEdrRequestSchema.NEGOTIATE_EDR_REQUEST_EXAMPLE;

public class EdrSchema {

    @Schema(name = "NegotiateEdrRequest", example = NEGOTIATE_EDR_REQUEST_EXAMPLE)
    public record NegotiateEdrRequestSchema(
            @Schema(name = TYPE, example = NegotiateEdrRequestDto.EDR_REQUEST_DTO_TYPE)
            String type,
            String protocol,
            String connectorAddress,
            @Deprecated(since = "0.1.3")
            @Schema(deprecated = true, description = "please use providerId instead")
            String connectorId,
            String providerId,
            ContractNegotiationApi.ContractOfferDescriptionSchema offer,
            List<ManagementApiSchema.CallbackAddressSchema> callbackAddresses) {

        public static final String NEGOTIATE_EDR_REQUEST_EXAMPLE = """
                {
                    "@context": { "edc": "https://w3id.org/edc/v0.0.1/ns/" },
                    "@type": "NegotiateEdrRequestDto",
                    "counterPartyAddress": "http://provider-address",
                    "protocol": "dataspace-protocol-http",
                    "providerId": "provider-id",
                    "offer": {
                        "offerId": "offer-id",
                        "assetId": "asset-id",
                        "policy": {
                            "@context": "http://www.w3.org/ns/odrl.jsonld",
                            "@type": "Set",
                            "@id": "offer-id",
                            "permission": [{
                                "target": "asset-id",
                                "action": "display"
                            }]
                        }
                    },
                    "callbackAddresses": [{
                        "transactional": false,
                        "uri": "http://callback/url",
                        "events": ["contract.negotiation", "transfer.process"]
                    }]
                }
                """;
    }

    @Schema(name = "EndpointDataReferenceEntry", example = ENDPOINT_DATA_REFERENCE_ENTRY_EXAMPLE)
    public record EndpointDataReferenceEntrySchema(
            @Schema(name = TYPE, example = EndpointDataReferenceEntry.SIMPLE_TYPE)
            String type,
            String agreementId,
            String assetId,
            String providerId,
            String edrState,
            Long expirationDate
    ) {
        public static final String ENDPOINT_DATA_REFERENCE_ENTRY_EXAMPLE = """
                {
                    "@type": "tx:EndpointDataReferenceEntry",
                    "agreementId": "MQ==:MQ==:ZTY3MzQ4YWEtNTdmZC00YzA0LTg2ZmQtMGMxNzk0MWM3OTkw",
                    "transferProcessId": "78a66945-d638-4c0a-be71-b35a0318a410",
                    "assetId": "1",
                    "providerId": "BPNL00DATAP00001",
                    "tx:edrState": "NEGOTIATED",
                    "tx:expirationDate": 1690811364000,
                    "@context": {
                        "dct": "https://purl.org/dc/terms/",
                        "tx": "https://w3id.org/tractusx/v0.0.1/ns/",
                        "edc": "https://w3id.org/edc/v0.0.1/ns/",
                        "dcat": "https://www.w3.org/ns/dcat/",
                        "odrl": "http://www.w3.org/ns/odrl/2/",
                        "dspace": "https://w3id.org/dspace/v0.8/"
                    }
                }
                """;
    }

}
