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

package org.eclipse.tractusx.edc.api.cp.adapter;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.model.DataAddressDto;
import org.eclipse.edc.api.model.IdResponseDto;
import org.eclipse.edc.web.spi.ApiErrorDetail;
import org.eclipse.tractusx.edc.api.cp.adapter.dto.EndpointDataReferenceEntryDto;
import org.eclipse.tractusx.edc.api.cp.adapter.dto.NegotiateEdrRequestDto;

import java.util.List;

@OpenAPIDefinition
@Tag(name = "Control Plane Adapter EDR Api")
public interface AdapterEdrApi {

    @Operation(description = "Initiates an EDR negotiation by handling a contract negotiation first and then a transfer process for a given offer and with the given counter part. Please note that successfully invoking this endpoint " +
            "only means that the negotiation was initiated.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The negotiation was successfully initiated.",
                            content = @Content(schema = @Schema(implementation = IdResponseDto.class))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
            })
    JsonObject initiateEdrNegotiation(@Schema(implementation = NegotiateEdrRequestDto.class) JsonObject dto);

    @Operation(description = "Returns all EndpointDataReference entry according to a query",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = EndpointDataReferenceEntryDto.class)))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))}
    )
    List<JsonObject> queryEdrs(String assetId, String agreementId);

    @Operation(description = "Gets an EDR  with the given transfer process ID)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The EDR cached",
                            content = @Content(schema = @Schema(implementation = DataAddressDto.class))),
                    @ApiResponse(responseCode = "400", description = "Request was malformed, e.g. id was null",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "404", description = "An EDR with the given ID does not exist",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))
            }
    )
    JsonObject getEdr(String transferProcessId);
}
