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

package org.eclipse.tractusx.edc.api.edr;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.model.ApiCoreSchema;
import org.eclipse.edc.connector.api.management.configuration.ManagementApiSchema;
import org.eclipse.edc.web.spi.ApiErrorDetail;
import org.eclipse.tractusx.edc.api.edr.schema.EdrSchema;

@OpenAPIDefinition
@Tag(name = "Control Plane EDR Api")
public interface EdrApi {

    @Operation(description = "Initiates an EDR negotiation by handling a contract negotiation first and then a transfer process for a given offer and with the given counter part. Please note that successfully invoking this endpoint " +
            "only means that the negotiation was initiated.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The negotiation was successfully initiated.",
                            content = @Content(schema = @Schema(implementation = ApiCoreSchema.IdResponseSchema.class))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
            })
    JsonObject initiateEdrNegotiation(@Schema(implementation = EdrSchema.NegotiateEdrRequestSchema.class) JsonObject dto);

    @Operation(description = "Returns all EndpointDataReference entry according to a query",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The ERD cache was retrieved",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = EdrSchema.EndpointDataReferenceEntrySchema.class)))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))) }
    )
    JsonArray queryEdrs(String assetId, String agreementId, String providerId);

    @Operation(description = "Gets an EDR with the given transfer process ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The EDR cached",
                            content = @Content(schema = @Schema(implementation = ManagementApiSchema.DataAddressSchema.class))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "404", description = "An EDR with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))
            }
    )
    JsonObject getEdr(String transferProcessId);

    @Operation(description = "Delete an EDR with the given transfer process ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The EDR cached was deleted successfully"),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "404", description = "An EDR with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))
            }
    )
    void deleteEdr(String transferProcessId);
}
