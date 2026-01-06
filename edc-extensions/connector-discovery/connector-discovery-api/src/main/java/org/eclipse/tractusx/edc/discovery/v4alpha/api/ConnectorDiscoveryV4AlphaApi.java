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

package org.eclipse.tractusx.edc.discovery.v4alpha.api;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.json.JsonObject;
import org.eclipse.edc.web.spi.ApiErrorDetail;
import org.eclipse.tractusx.edc.discovery.v4alpha.spi.ConnectorParamsDiscoveryRequest;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_NAMESPACE;

@OpenAPIDefinition(info = @Info(description = "With this API clients discover EDC requesting parameters according to different DSP versions", title = "Connector Discovery API"))
@Tag(name = "Connector Discovery")
public interface ConnectorDiscoveryV4AlphaApi {

    @Operation(description = "Discover supported connector parameters.",
            requestBody = @RequestBody(content = @Content(schema = @Schema(name = "Connector Params Discovery Request", implementation = ConnectorParamsDiscoveryRequestSchema.class))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "A list of connector parameters per DSP version",
                            content = @Content(schema = @Schema(name = "Connector Discovery Response", implementation = ConnectorDiscoveryResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Discovery failed due to an internal error",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "502", description = "Discovery failed due to connection to counter party",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))
            })
    JsonObject discoverDspVersionParamsV4Alpha(JsonObject querySpecJson);

    @Operation(description = "Retrieves 'DataService' Entries from the DID document of a participant and provides the connection parameters for all found",
            requestBody = @RequestBody(content = @Content(schema = @Schema(name = "Service Discovery Request", implementation = ServiceDiscoveryRequestSchema.class))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "A list of connector endpoint parameters for the right version for each found connector",
                            content = @Content(array = @ArraySchema(schema = @Schema(name = "Service Discovery Response", implementation = ConnectorDiscoveryResponse.class)))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "404", description = "Given identifier could not be resolved",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "500", description = "Discovery failed due to an internal error",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "502", description = "Discovery failed due to connection to counter party",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))
            })
    JsonObject discoverConnectorServicesV4Alpha(JsonObject querySpecJson);

    @Schema(name = "ConnectorParamsDiscoveryRequest", example = ConnectorParamsDiscoveryRequestSchema.EXAMPLE)
    record ConnectorParamsDiscoveryRequestSchema(
            @Schema(name = CONTEXT, requiredMode = REQUIRED)
            Object context,
            @Schema(name = TYPE, example = ConnectorParamsDiscoveryRequest.TYPE)
            String type,
            @Schema(requiredMode = REQUIRED)
            String bpnl,
            @Schema(requiredMode = REQUIRED)
            String counterPartyAddress
    ) {
        public static final String EXAMPLE = """
                {
                    "@context": {
                        "tx": "https://w3id.org/tractusx/v0.0.1/ns/",
                        "edc": "https://w3id.org/edc/v0.0.1/ns/"
                    },
                    "@type": "tx:ConnectorParamsDiscoveryRequest",
                    "tx:bpnl": "BPNL1234567890",
                    "edc:counterPartyAddress": "https://provider.domain.com/api/dsp"
                }
                """;
    }

    @Schema(name = "ConnectorDiscoveryResponse", example = ConnectorDiscoveryResponse.EXAMPLE)
    record ConnectorDiscoveryResponse(
            Object context,
            String counterPartyId,
            String counterPartyAddress,
            String protocol
    ) {

        public static final String EXAMPLE = """
                {
                    "@context": {
                        "edc": "https://w3id.org/edc/v0.0.1/ns/",
                        "tx": "https://w3id.org/tractusx/v0.0.1/ns/"
                    },
                    "edc:counterPartyId": "did:web:one-example.com",
                    "edc:counterPartyAddress": "https://provider.domain.com/api/dsp/2025-1",
                    "edc:protocol": "dataspace-protocol-http:2025-1"
                }
                """;
    }

    @Schema(name = "ServiceDiscoveryRequestSchema", example = ServiceDiscoveryRequestSchema.EXAMPLE)
    record ServiceDiscoveryRequestSchema(
            @Schema(name = CONTEXT, requiredMode = REQUIRED)
            Object context,
            @Schema(name = TYPE, example = CSDR_TYPE)
            String type,
            @Schema(requiredMode = REQUIRED)
            String identifier,
            @Schema(requiredMode = NOT_REQUIRED)
            String[] knowns
    ) {
        public static final String CSDR_TYPE = TX_NAMESPACE +  "ConnectorServiceDiscoveryRequest";
        public static final String SERVICE_DISCOVERY_REQUEST_IDENTIFIER_ATTRIBUTE = TX_NAMESPACE + "identifier";
        public static final String SERVICE_DISCOVERY_REQUEST_KNOWNS_ATTRIBUTE = TX_NAMESPACE + "knowns";

        public static final String EXAMPLE = """
                    {
                        "@context": {
                            "tx": "https://w3id.org/tractusx/v0.0.1/ns/"
                        },
                        "@type": "tx:ConnectorParamsDiscoveryRequest",
                        "tx:identifier": "did:web:one-example.com",
                        "tx:knowns": [
                            "https://provider.domain.com/conn1/api/dsp",
                            "https://provider.domain.com/conn2/api/v1/dsp",
                        ]
                    }
                    """;
    }
}
