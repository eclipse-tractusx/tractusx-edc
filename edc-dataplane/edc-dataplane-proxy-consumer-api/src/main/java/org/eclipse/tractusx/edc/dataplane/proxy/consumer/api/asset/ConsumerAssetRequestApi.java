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

package org.eclipse.tractusx.edc.dataplane.proxy.consumer.api.asset;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import org.eclipse.tractusx.edc.dataplane.proxy.consumer.api.asset.model.AssetRequest;

/**
 *
 */
@OpenAPIDefinition
@Tag(name = "Data Plane Proxy API")
public interface ConsumerAssetRequestApi {

    @Operation(responses = {
            @ApiResponse(content = @Content(mediaType = "application/json", schema = @Schema(implementation = AssetRequest.class)), description = "Requests asset data")
    })
    void requestAsset(AssetRequest request, @Suspended AsyncResponse response);
}
