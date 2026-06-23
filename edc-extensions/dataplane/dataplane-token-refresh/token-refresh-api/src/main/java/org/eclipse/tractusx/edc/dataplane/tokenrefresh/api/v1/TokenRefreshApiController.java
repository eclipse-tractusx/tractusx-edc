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

import io.swagger.v3.oas.annotations.parameters.RequestBody;
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

import java.util.HashMap;
import java.util.Map;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;

@Produces({ MediaType.APPLICATION_JSON })
@Path("/token")
public class TokenRefreshApiController implements TokenRefreshApi {
    private static final String GRANT_TYPE = "grant_type";
    private static final String REFRESH_TOKEN = "refresh_token";
    private final DataPlaneTokenRefreshService tokenRefreshService;

    public TokenRefreshApiController(DataPlaneTokenRefreshService tokenRefreshService) {
        this.tokenRefreshService = tokenRefreshService;
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Override
    public TokenResponse refreshToken(@QueryParam(GRANT_TYPE) String grantType,
                                      @QueryParam(REFRESH_TOKEN) String refreshToken,
                                      @HeaderParam(AUTHORIZATION) String bearerToken,
                                      @RequestBody() String formParams) {
        var paramMap = parseFormParameter(formParams);
        if (!paramMap.containsKey(GRANT_TYPE)) {
            paramMap.put(GRANT_TYPE, grantType);
        }
        if (!paramMap.containsKey(REFRESH_TOKEN)) {
            paramMap.put(REFRESH_TOKEN, refreshToken);
        }

        if (!REFRESH_TOKEN.equals(paramMap.get(GRANT_TYPE))) {
            throw new InvalidRequestException("Grant type MUST be '%s' but was '%s'".formatted(REFRESH_TOKEN, paramMap.get(GRANT_TYPE)));
        }
        if (StringUtils.isNullOrBlank(paramMap.get(REFRESH_TOKEN))) {
            throw new InvalidRequestException("Parameter 'refresh_token' cannot be null");
        }
        if (StringUtils.isNullOrBlank(bearerToken)) {
            throw new AuthenticationFailedException("Authorization header missing");
        }

        return tokenRefreshService.refreshToken(paramMap.get(REFRESH_TOKEN), bearerToken)
                .orElseThrow(f -> new AuthenticationFailedException(f.getFailureDetail()));
    }

    private Map<String, String> parseFormParameter(String formParams) {
        var result = new HashMap<String, String>();
        if (!StringUtils.isNullOrBlank(formParams)) {
            var params = formParams.split("&");
            for (String param : params) {
                if (!StringUtils.isNullOrBlank(param)) {
                    var keyValuePair = param.split("=");
                    if (keyValuePair.length == 2) {
                        result.put(keyValuePair[0], keyValuePair[1]);
                    }
                }
            }
        }
        return result;
    }
}
