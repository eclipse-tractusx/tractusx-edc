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

package org.eclipse.tractusx.edc.api.edr.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import org.eclipse.edc.connector.api.management.configuration.ManagementApiSchema;
import org.eclipse.edc.connector.api.management.contractnegotiation.ContractNegotiationApi;
import org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry;

import java.util.List;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.tractusx.edc.api.edr.dto.NegotiateEdrRequestDto.EDR_REQUEST_DTO_TYPE;
import static org.eclipse.tractusx.edc.api.edr.schema.EdrSchema.EndpointDataReferenceEntrySchema.ENDPOINT_DATA_REFERENCE_ENTRY_EXAMPLE;
import static org.eclipse.tractusx.edc.api.edr.schema.EdrSchema.NegotiateEdrRequestSchema.NEGOTIATE_EDR_REQUEST_EXAMPLE;

public class EdrSchema {

    @Schema(name = "NegotiateEdrRequest", example = NEGOTIATE_EDR_REQUEST_EXAMPLE)
    public record NegotiateEdrRequestSchema(
            @Schema(name = TYPE, example = EDR_REQUEST_DTO_TYPE)
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
                    "connectorAddress": "http://provider-address",
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
                    "edc:agreementId": "MQ==:MQ==:ZTY3MzQ4YWEtNTdmZC00YzA0LTg2ZmQtMGMxNzk0MWM3OTkw",
                    "edc:transferProcessId": "78a66945-d638-4c0a-be71-b35a0318a410",
                    "edc:assetId": "1",
                    "edc:providerId": "BPNL00DATAP00001",
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
