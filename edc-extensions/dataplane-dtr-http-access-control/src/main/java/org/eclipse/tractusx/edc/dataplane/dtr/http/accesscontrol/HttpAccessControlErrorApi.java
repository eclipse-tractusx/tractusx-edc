/********************************************************************************
 * Copyright (c) 2024 Robert Bosch Manufacturing Solutions GmbH and others
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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
 ********************************************************************************/

package org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

@OpenAPIDefinition(info = @Info(description = "With this API we can return custom error responses in case of authentication errors.", title = "HTTP Access Control Error API"))
@Tag(name = "DTR HTTP Access Control")
public interface HttpAccessControlErrorApi {

    @Operation(description = "Return 401 error",
            parameters = @Parameter(name = "reasonPhrase", required = true, schema = @Schema(implementation = String.class)),
            responses = {
                    @ApiResponse(responseCode = "401", description = "The request cannot be allowed as the user is unauthorized.")
            })
    Response unauthorized(String reasonPhrase);

    @Operation(description = "Return 403 error",
            parameters = @Parameter(name = "reasonPhrase", required = true, schema = @Schema(implementation = String.class)),
            responses = {
                    @ApiResponse(responseCode = "403", description = "The request cannot be allowed as the user is not allowed to see the target.")
            })
    @GET
    @Path("/403")
    Response forbidden(@QueryParam("reasonPhrase") String reasonPhrase);

    @Operation(description = "Return 200 status",
            requestBody = @RequestBody(required = true, description = "The desired response body."),
            responses = {
                    @ApiResponse(responseCode = "200", description = "The provided response body is used.")
            })
    @POST
    @Path("/200")
    Response ok(@QueryParam("body") String body);
}
