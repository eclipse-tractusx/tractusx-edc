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

package org.eclipse.tractusx.edc.dataplane.tokenrefresh.api.v1;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.eclipse.edc.web.spi.ApiErrorDetail;
import org.eclipse.tractusx.edc.spi.tokenrefresh.dataplane.model.TokenResponse;

@SecurityScheme(name = "Authentication",
        description = "Self-Issued ID token containing a token",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
@OpenAPIDefinition(info = @Info(description = "With this API clients can refresh their access token for a provider's HTTP data plane using an authentication token and a refresh token.", title = "Token Refresh API"))
@Tag(name = "Token Refresh API")
public interface TokenRefreshApi {

    @Operation(description = "Resolves all groups for a particular BPN",
            parameters = { @Parameter(name = "grant_type", description = "The grant type. Must be \"refresh_token\""),
                    @Parameter(name = "refresh_token", description = "The refresh token") },
            responses = {
                    @ApiResponse(responseCode = "200", description = "The access token and refresh token were updated. Expiry should be " +
                            "interpreted as starting from the time of message reception, allowing for some leeway.",
                            content = @Content(schema = @Schema(implementation = TokenResponse.class))),
                    @ApiResponse(responseCode = "401", description = "The token could not be refreshed due to an authentication error, either the refresh token or the Authorization header were invalid.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class)))),
                    @ApiResponse(responseCode = "400", description = "Request body was malformed, query parameters were missing, etc.",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiErrorDetail.class))))
            })
    TokenResponse refreshToken(String grantType, String refreshToken, String bearerToken);
}
