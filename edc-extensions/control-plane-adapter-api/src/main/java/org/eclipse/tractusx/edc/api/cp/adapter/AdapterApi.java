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
import jakarta.validation.Valid;
import org.eclipse.edc.web.spi.ApiErrorDetail;
import org.eclipse.tractusx.edc.api.cp.adapter.dto.TransferOpenRequestDto;

@OpenAPIDefinition
@Tag(name = "Control Plane Adapter Api")
public interface AdapterApi {

    @Operation(description = "Initiates a contract negotiation and a transfer process for a given offer and with the given counter part. Please note that successfully invoking this endpoint " +
            "only means that the negotiation was initiated.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "The negotiation was successfully initiated."),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
            })
    void openTransfer(@Valid TransferOpenRequestDto dto);
}
