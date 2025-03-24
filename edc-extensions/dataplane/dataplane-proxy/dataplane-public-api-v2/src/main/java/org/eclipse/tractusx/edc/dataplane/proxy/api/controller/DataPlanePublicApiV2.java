/**
 * Copyright (c) 2022 Amadeus - Initial implementation
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - handle HEAD requests
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
 **/

package org.eclipse.tractusx.edc.dataplane.proxy.api.controller;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.ContainerRequestContext;

@OpenAPIDefinition
@Tag(name = "Data Plane public API",
        description = "The public API of the Data Plane is a data proxy enabling a data consumer to actively query" +
                "data from the provider data source (e.g. backend Rest API, internal database...) through its Data Plane" +
                "instance. Thus the Data Plane is the only entry/output door for the data, which avoids the provider to expose" +
                "directly its data externally." +
                "The Data Plane public API being a proxy, it supports all verbs (i.e. GET, POST, PUT, PATCH, DELETE), which" +
                "can then conveyed until the data source is required. This is especially useful when the actual data source" +
                "is a Rest API itself." +
                "In the same manner, any set of arbitrary query parameters, path parameters and request body are supported " +
                "(in the limits fixed by the HTTP server) and can also conveyed to the actual data source.")
public interface DataPlanePublicApiV2 {

    @Operation(description = "Send `GET` data query to the Data Plane.",
            responses = {
                    @ApiResponse(responseCode = "400", description = "Missing access token"),
                    @ApiResponse(responseCode = "403", description = "Access token is expired or invalid"),
                    @ApiResponse(responseCode = "500", description = "Failed to transfer data")
            }
    )
    void get(ContainerRequestContext context, AsyncResponse response);

    @Operation(description = "Send `HEAD` data query to the Data Plane.",
            responses = {
                    @ApiResponse(responseCode = "400", description = "Missing access token"),
                    @ApiResponse(responseCode = "403", description = "Access token is expired or invalid"),
                    @ApiResponse(responseCode = "500", description = "Failed to transfer data")
            }
    )
    void head(ContainerRequestContext context, AsyncResponse response);

    @Operation(description = "Send `POST` data query to the Data Plane.",
            responses = {
                    @ApiResponse(responseCode = "400", description = "Missing access token"),
                    @ApiResponse(responseCode = "403", description = "Access token is expired or invalid"),
                    @ApiResponse(responseCode = "500", description = "Failed to transfer data")
            }
    )
    void post(ContainerRequestContext context, AsyncResponse response);

    @Operation(description = "Send `PUT` data query to the Data Plane.",
            responses = {
                    @ApiResponse(responseCode = "400", description = "Missing access token"),
                    @ApiResponse(responseCode = "403", description = "Access token is expired or invalid"),
                    @ApiResponse(responseCode = "500", description = "Failed to transfer data")
            }
    )
    void put(ContainerRequestContext context, AsyncResponse response);

    @Operation(description = "Send `DELETE` data query to the Data Plane.",
            responses = {
                    @ApiResponse(responseCode = "400", description = "Missing access token"),
                    @ApiResponse(responseCode = "403", description = "Access token is expired or invalid"),
                    @ApiResponse(responseCode = "500", description = "Failed to transfer data")
            }
    )
    void delete(ContainerRequestContext context, AsyncResponse response);

    @Operation(description = "Send `PATCH` data query to the Data Plane.",
            responses = {
                    @ApiResponse(responseCode = "400", description = "Missing access token"),
                    @ApiResponse(responseCode = "403", description = "Access token is expired or invalid"),
                    @ApiResponse(responseCode = "500", description = "Failed to transfer data")
            }
    )
    void patch(ContainerRequestContext context, AsyncResponse response);
}
