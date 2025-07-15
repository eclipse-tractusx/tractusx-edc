/********************************************************************************
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
 ********************************************************************************/

package org.eclipse.tractusx.edc.dataflow.api.v4alpha;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.links.Link;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.eclipse.edc.api.model.ApiCoreSchema;

@OpenAPIDefinition(info = @Info(version = "v4alpha"))
@Tag(name = "DataFlow API V4Alpha")
public interface DataFlowApi {

    String ASYNC_WARNING = "Due to the asynchronous nature of transfers, a successful response only indicates that the request was successfully received.";

    @Operation(description = "Requests the trigger of a data transfer. " + ASYNC_WARNING,
            responses = {
                    @ApiResponse(responseCode = "204", description = "Request was sucessfully received", links = @Link(name = "poll-state", operationId = "triggerDataTransferV4Alpha")),
                    @ApiResponse(responseCode = "400", description = "Request was malformed", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "404", description = "Data flow with the given ID does not exist", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class)))),
                    @ApiResponse(responseCode = "409", description = "Data flow is not in a required state", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiCoreSchema.ApiErrorDetailSchema.class))))
            }
    )
    void triggerDataTransferV4Alpha(String id);

}
