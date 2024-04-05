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

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.util.string.StringUtils;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.tractusx.edc.spi.tokenrefresh.dataplane.DataPlaneTokenRefreshService;
import org.eclipse.tractusx.edc.spi.tokenrefresh.dataplane.model.TokenResponse;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;

@Produces({ MediaType.APPLICATION_JSON })
@Path("/token")
public class TokenRefreshApiController implements TokenRefreshApi {
    private static final String REFRESH_TOKEN_GRANT = "refresh_token";
    private final DataPlaneTokenRefreshService tokenRefreshService;

    public TokenRefreshApiController(DataPlaneTokenRefreshService tokenRefreshService) {
        this.tokenRefreshService = tokenRefreshService;
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Override
    public TokenResponse refreshToken(@QueryParam("grant_type") String grantType,
                                      @QueryParam("refresh_token") String refreshToken,
                                      @HeaderParam(AUTHORIZATION) String bearerToken) {
        if (!REFRESH_TOKEN_GRANT.equals(grantType)) {
            throw new InvalidRequestException("Grant type MUST be '%s' but was '%s'".formatted(REFRESH_TOKEN_GRANT, grantType));
        }
        if (StringUtils.isNullOrBlank(refreshToken)) {
            throw new InvalidRequestException("Parameter 'refresh_token' cannot be null");
        }
        if (StringUtils.isNullOrBlank(bearerToken)) {
            throw new AuthenticationFailedException("Authorization header missing");
        }

        return tokenRefreshService.refreshToken(refreshToken, bearerToken)
                .orElseThrow(f -> new AuthenticationFailedException(f.getFailureDetail()));
    }
}
