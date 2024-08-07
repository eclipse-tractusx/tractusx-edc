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

package org.eclipse.tractusx.edc.api.edr.v3;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.model.ApiCoreSchema;
import org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.v3.ContractNegotiationApiV3;
import org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.eclipse.edc.web.spi.ApiErrorDetail;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

@OpenAPIDefinition
@Tag(name = "Control Plane EDR Api")
public interface EdrCacheApiV3 {

    @Operation(description = "Initiates an EDR negotiation by handling a contract negotiation first and then a transfer process for a given offer and with the given counter part. Please note that successfully invoking this endpoint " +
            "only means that the negotiation was initiated.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The negotiation was successfully initiated.",
                            content = @Content(schema = @Schema(implementation = ApiCoreSchema.IdResponseSchema.class))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
            })
    JsonObject initiateEdrNegotiationV3(@Schema(implementation = ContractNegotiationApiV3.ContractRequestSchema.class) JsonObject dto);

    @Operation(description = "Request all Edr entries according to a particular query",
            requestBody = @RequestBody(
                    content = @Content(schema = @Schema(implementation = ApiCoreSchema.QuerySpecSchema.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The edr entries matching the query",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = EndpointDataReferenceEntrySchema.class)))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            })
    JsonArray requestEdrEntriesV3(JsonObject querySpecJson);

    @Operation(description = "Gets the EDR data address with the given transfer process ID",
            parameters = { @Parameter(name = "transferProcessId", description = "The ID of the transferprocess for which the EDR should be fetched", required = true),
                    @Parameter(name = "auto_refresh", description = "Whether the access token that is stored on the EDR should be checked for expiry, and renewed if necessary. Default is true.")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "The data address",
                            content = @Content(schema = @Schema(implementation = ApiCoreSchema.DataAddressSchema.class))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "404", description = "An EDR data address with the given transfer process ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            }
    )
    JsonObject getEdrEntryDataAddressV3(String transferProcessId, boolean autoRefresh);

    @Operation(description = "Removes an EDR entry given the transfer process ID",
            responses = {
                    @ApiResponse(responseCode = "204", description = "EDR entry was deleted successfully"),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "404", description = "An EDR entry with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            })
    void removeEdrEntryV3(String transferProcessId);

    @Operation(description = "Refreshes and returns the EDR data address with the given transfer process ID",
            parameters = { @Parameter(name = "transferProcessId", description = "The ID of the transferprocess for which the EDR should be fetched", required = true),
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "The data address",
                            content = @Content(schema = @Schema(implementation = ApiCoreSchema.DataAddressSchema.class))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "404", description = "An EDR data address with the given transfer process ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            }
    )
    JsonObject refreshEdrV3(String transferProcessId);


    @ArraySchema()
    @Schema(name = "EndpointDataReferenceEntry", example = EndpointDataReferenceEntrySchema.EDR_ENTRY_OUTPUT_EXAMPLE)
    record EndpointDataReferenceEntrySchema(
            @Schema(name = ID)
            String id,
            @Schema(name = TYPE, example = EndpointDataReferenceEntry.EDR_ENTRY_TYPE)
            String type
    ) {
        public static final String EDR_ENTRY_OUTPUT_EXAMPLE = """
                {
                    "@context": { "@vocab": "https://w3id.org/edc/v0.0.1/ns/" },
                    "@id": "transfer-process-id",
                    "transferProcessId": "transfer-process-id",
                    "agreementId": "agreement-id",
                    "contractNegotiationId": "contract-negotiation-id",
                    "assetId": "asset-id",
                    "providerId": "provider-id",
                    "createdAt": 1688465655
                }
                """;
    }
}
