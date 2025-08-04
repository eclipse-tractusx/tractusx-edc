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
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.web.spi.ApiErrorDetail;
import org.eclipse.tractusx.edc.discovery.models.ConnectorDiscoveryResponse;
import org.eclipse.tractusx.edc.discovery.models.ConnectorParamsDiscoveryRequest;


@OpenAPIDefinition(info = @Info(description = "With this API clients discover EDC requesting parameters according to different DSP versions", title = "Connector Discovery API"))
@Tag(name = "Connector Discovery")
public interface ConnectorDiscoveryV4AlphaApi {

    @Operation(description = "Discover all known connector parameters.",
            requestBody = @RequestBody(content = @Content(schema = @Schema(name = "Connector Discovery Request", example = ConnectorParamsDiscoveryRequest.EXAMPLE))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "A list of connector parameters per DSP version",
                            content = @Content(array = @ArraySchema(schema = @Schema(name = "Connector Discovery Response", example = ConnectorDiscoveryResponse.EXAMPLE)))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))
            })
    JsonArray discoverConnectorV3(JsonObject querySpecJson);

}
