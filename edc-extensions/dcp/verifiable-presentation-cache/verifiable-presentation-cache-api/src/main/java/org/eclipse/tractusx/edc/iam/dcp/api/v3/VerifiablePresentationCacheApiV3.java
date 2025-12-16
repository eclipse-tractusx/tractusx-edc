/********************************************************************************
 * Copyright (c) 2025 Cofinity-X GmbH
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

package org.eclipse.tractusx.edc.iam.dcp.api.v3;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@OpenAPIDefinition(info = @Info(description = "With this API clients can interact with the Verifiable Presentation Cache to remove cached entries for a participant.", title = "Verifiable Presentation Cache API"))
@Tag(name = "Verifiable Presentation Cache")
public interface VerifiablePresentationCacheApiV3 {

    @Operation(description = "Removes all cached entries for a participant by ID.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Entries for the participant were successfully deleted."),
                    @ApiResponse(responseCode = "500", description = "An error occurred removing the entries from the cache.")
            })
    void removeCacheEntries(String participantId);

}
