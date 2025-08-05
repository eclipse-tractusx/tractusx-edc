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

package org.eclipse.tractusx.edc.discovery.api;

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
import org.eclipse.tractusx.edc.discovery.models.ConnectorParamsDiscoveryRequest;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;


@OpenAPIDefinition(info = @Info(description = "With this API clients discover EDC requesting parameters according to different DSP versions", title = "Connector Discovery API"))
@Tag(name = "Connector Discovery")
public interface ConnectorDiscoveryV4AlphaApi {

    @Operation(description = "Discover supported connector parameters.",
            requestBody = @RequestBody(content = @Content(schema = @Schema(name = "Connector Params Discovery Request", implementation = ConnectorParamsDiscoveryRequestSchema.class))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "A list of connector parameters per DSP version",
                            content = @Content(array = @ArraySchema(schema = @Schema(name = "Connector Discovery Response", implementation = ConnectorDiscoveryResponse.class)))),
                    @ApiResponse(responseCode = "500", description = "Discovery failed due to an internal error",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "502", description = "Discovery failed due to connection to counter party",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))
            })
    JsonObject discoverDspVersionParamsV3(JsonObject querySpecJson);


    @Schema(name = "ConnectorParamsDiscoveryRequestSchema", example = ConnectorParamsDiscoveryRequestSchema.EXAMPLE)
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
                        "edc": "https://w3id.org/edc/v0.0.1/ns/",
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

}
