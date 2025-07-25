/*
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft
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

package org.eclipse.tractusx.edc.mock.api.instrumentation;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.eclipse.edc.web.spi.ApiErrorDetail;
import org.eclipse.tractusx.edc.mock.RecordedRequest;

import java.util.List;

/**
 * Instrumentation API for the mock connector.
 *
 * @deprecated since 0.11.0
 */
@Deprecated(since = "0.11.0")
@OpenAPIDefinition(info = @Info(description = "This API allows to insert ", title = "Business Partner Group API"))
@Tag(name = "Business Partner Group")
public interface InstrumentationApi {

    @Operation(description = "Adds a new RecordedRequest to the end of the queue.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "The negotiation was successfully initiated."),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
            })
    void addNewRequest(RecordedRequest<?, ?> recordedRequest);

    @Operation(description = "Clears the entire request queue.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "The queue was successfully cleared.")
            })
    void clearQueue();

    @Operation(description = "Return the entire request queue.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The list of RecordedRequest objects.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = RecordedRequest.class)))),
            })
    List<RecordedRequest<?, ?>> getRequests();

    @Operation(description = "Return amount of items currently in the queue.",
            responses = {
                    @ApiResponse(responseCode = "200")
            })
    int count();
}
