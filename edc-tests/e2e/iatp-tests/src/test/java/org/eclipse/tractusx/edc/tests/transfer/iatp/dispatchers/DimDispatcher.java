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

package org.eclipse.tractusx.edc.tests.transfer.iatp.dispatchers;

import com.github.tomakehurst.wiremock.extension.ResponseTransformerV2;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.eclipse.edc.iam.identitytrust.sts.service.EmbeddedSecureTokenService;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.types.TypeManager;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.eclipse.edc.iam.identitytrust.spi.SelfIssuedTokenConstants.PRESENTATION_TOKEN_CLAIM;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUER;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SUBJECT;

/**
 * Mock service for DIM interaction. Underlying it uses the {@link EmbeddedSecureTokenService} for generating SI tokens
 */
public class DimDispatcher implements ResponseTransformerV2 {

    private static final TypeManager MAPPER = new JacksonTypeManager();
    private final String path;
    private final Map<String, EmbeddedSecureTokenService> secureTokenServices;

    public DimDispatcher(Map<String, EmbeddedSecureTokenService> secureTokenServices) {
        this("/", secureTokenServices);
    }

    public DimDispatcher(String path, Map<String, EmbeddedSecureTokenService> secureTokenServices) {
        this.path = path;
        this.secureTokenServices = secureTokenServices;
    }

    @Override
    public String getName() {
        return "dim-dispatcher";
    }

    @Override
    public Response transform(Response response, ServeEvent serveEvent) {
        var request = serveEvent.getRequest();
        var reqPathOnly = request.getUrl().split("\\?")[0];
        if (!reqPathOnly.equals(path)) {
            return notFound(response);
        }

        Map<String, Object> body = MAPPER.readValue(request.getBody(), Map.class);

        Optional<Response> grant = Optional.ofNullable(body.get("grantAccess"))
                .map(payload -> grantAccessHandler((Map<String, Object>) payload, response));

        Optional<Response> sign = Optional.ofNullable(body.get("signToken"))
                .map(payload -> signTokenHandler((Map<String, Object>) payload, response));

        return grant.or(() -> sign).orElse(notFound(response));
    }

    @Override
    public boolean applyGlobally() {
        return false;
    }

    @SuppressWarnings("unchecked")
    private Response grantAccessHandler(Map<String, Object> params, Response base) {
        var issuer = String.valueOf(params.get("consumerDid"));
        var audience = String.valueOf(params.get("providerDid"));

        @SuppressWarnings("unchecked")
        Collection<String> scopes = (Collection<String>) params.get("credentialTypes");
        var scope = scopes.stream().map("org.eclipse.tractusx.vc.type:%s:read"::formatted).collect(Collectors.joining(" "));
        var claims = Map.of(ISSUER, issuer, SUBJECT, issuer, AUDIENCE, audience);

        var sts = secureTokenServices.get(issuer);
        var token = sts.createToken(issuer, claims, scope)
                .map(TokenRepresentation::getToken)
                .orElseThrow(f -> new RuntimeException(f.getFailureDetail()));

        return jsonOk(base, Map.of("jwt", token));
    }

    private Response signTokenHandler(Map<String, Object> params, Response base) {
        var subject = String.valueOf(params.get("subject"));
        var accessToken = String.valueOf(params.get("token"));
        var audience = String.valueOf(params.get("audience"));
        var issuer = String.valueOf(params.get("issuer"));

        var claims = Map.of(ISSUER, issuer, SUBJECT, subject, AUDIENCE, audience, PRESENTATION_TOKEN_CLAIM, accessToken);

        var sts = secureTokenServices.get(issuer);
        var token = sts.createToken(issuer, claims, null)
                .map(TokenRepresentation::getToken)
                .orElseThrow(f -> new RuntimeException(f.getFailureDetail()));

        return jsonOk(base, Map.of("jwt", token));
    }

    private Response jsonOk(Response base, Object payload) {
        var json = MAPPER.writeValueAsString(payload);
        return Response.Builder.like(base)
                .but()
                .status(200)
                .headers(new HttpHeaders(HttpHeader.httpHeader("Content-Type", "application/json")))
                .body(json)
                .build();
    }

    private Response notFound(Response base) {
        return Response.Builder.like(base)
                .but()
                .status(404)
                .body("")
                .build();
    }
}
