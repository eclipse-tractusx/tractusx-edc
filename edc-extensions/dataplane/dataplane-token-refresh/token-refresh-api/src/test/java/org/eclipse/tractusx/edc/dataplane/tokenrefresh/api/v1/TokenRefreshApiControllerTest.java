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

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.eclipse.tractusx.edc.spi.tokenrefresh.dataplane.DataPlaneTokenRefreshService;
import org.eclipse.tractusx.edc.spi.tokenrefresh.dataplane.model.TokenResponse;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TokenRefreshApiControllerTest extends RestControllerTestBase {

    private final DataPlaneTokenRefreshService refreshService = mock();

    @DisplayName("Expect HTTP 400 when no Authorization header is present")
    @Test
    void refresh_noAuthHeader_expect401() {
        baseRequest()
                .queryParam("grant_type", "refresh_token")
                .queryParam("refresh_token", "foo-token")
                /* missing: .header(AUTHORIZATION, "auth-token") */
                .contentType(ContentType.URLENC)
                .then()
                .statusCode(401);
    }

    @DisplayName("Expect HTTP 200 when the token was successfully refreshed")
    @Test
    void refresh_expect200() {
        when(refreshService.refreshToken(any(), any())).thenReturn(Result.success(new TokenResponse("new-accesstoken", "new-refreshtoken", 3000L, "bearer")));
        baseRequest()
                .queryParam("grant_type", "refresh_token")
                .queryParam("refresh_token", "foo-token")
                .header(AUTHORIZATION, "auth-token")
                .contentType(ContentType.URLENC)
                .then()
                .statusCode(200)
                .body(Matchers.isA(TokenResponse.class));
    }

    @DisplayName("Expect HTTP 400 when an invalid grant type was provided")
    @ParameterizedTest(name = "Invalid grant_type: {0}")
    @ValueSource(strings = { "REFRESH_TOKEN", "refreshToken", "invalid_grant", "client_credentials", "" })
    @NullSource
    void refresh_invalidGrantType_expect400(String grant) {
        baseRequest()
                .queryParam("grant_type", grant)
                .queryParam("refresh_token", "foo-token")
                .header(AUTHORIZATION, "auth-token")
                .contentType(ContentType.URLENC)
                .then()
                .statusCode(400);
    }

    @DisplayName("Expect HTTP 400 when an invalid refresh token was provided")
    @ParameterizedTest(name = "Invalid refresh_token: {0}")
    @NullSource
    @EmptySource
    void refresh_invalidRefreshToken_expect400(String refreshToken) {
        baseRequest()
                .queryParam("grant_type", "refresh_token")
                .queryParam("refresh_token", refreshToken)
                .header(AUTHORIZATION, "auth-token")
                .contentType(ContentType.URLENC)
                .then()
                .statusCode(400);
    }

    @DisplayName("Expect HTTP 400 when one of the query params was missing")
    @Test
    void refresh_queryParamsMissing() {
        baseRequest()
                .queryParam("grant_type", "refresh_token")
                .header(AUTHORIZATION, "auth-token")
                .contentType(ContentType.URLENC)
                .then()
                .statusCode(400);

        baseRequest()
                .queryParam("refresh_token", "foo-token")
                .header(AUTHORIZATION, "auth-token")
                .contentType(ContentType.URLENC)
                .then()
                .statusCode(400);
    }

    @DisplayName("Expect HTTP 401 if the auth header or refresh token are invalid")
    @Test
    void refresh_tokenInvalid_expect401() {
        when(refreshService.refreshToken(any(), any())).thenReturn(Result.failure("Invalid auth token"));

        baseRequest()
                .queryParam("grant_type", "refresh_token")
                .queryParam("refresh_token", "foo-token")
                .header(AUTHORIZATION, "auth-token")
                .contentType(ContentType.URLENC)
                .then()
                .statusCode(401)
                .body(containsString("Invalid auth token"));
    }

    @Override
    protected Object controller() {
        return new TokenRefreshApiController(refreshService);
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .basePath("/token")
                .when();
    }

}