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

import org.eclipse.edc.iam.identitytrust.sts.service.EmbeddedSecureTokenService;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.types.TypeManager;
import org.mockserver.mock.action.ExpectationResponseCallback;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

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
public class DimDispatcher implements ExpectationResponseCallback {

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
    public HttpResponse handle(HttpRequest httpRequest) {
        if (httpRequest.getPath().getValue().split("\\?")[0].equals(path)) {

            var body = MAPPER.readValue(httpRequest.getBody().getRawBytes(), Map.class);

            var grant = Optional.ofNullable(body.get("grantAccess"))
                    .map((payload) -> grantAccessHandler((Map<String, Object>) payload));

            var sign = Optional.ofNullable(body.get("signToken"))
                    .map((payload) -> signTokenHandler((Map<String, Object>) payload));

            return grant.or(() -> sign).orElse(HttpResponse.response().withStatusCode(404));
        }
        return HttpResponse.response().withStatusCode(404);
    }

    @SuppressWarnings("unchecked")
    private HttpResponse grantAccessHandler(Map<String, Object> params) {
        var issuer = params.get("consumerDid").toString();
        var audience = params.get("providerDid").toString();
        Collection<String> scopes = (Collection<String>) params.get("credentialTypes");
        var scope = scopes.stream().map("org.eclipse.tractusx.vc.type:%s:read"::formatted).collect(Collectors.joining(" "));
        var claims = Map.of(ISSUER, issuer, SUBJECT, issuer, AUDIENCE, audience);

        var sts = secureTokenServices.get(issuer);
        var token = sts.createToken(issuer, claims, scope)
                .map(TokenRepresentation::getToken)
                .orElseThrow(failure -> new RuntimeException(failure.getFailureDetail()));

        return HttpResponse.response(MAPPER.writeValueAsString(Map.of("jwt", token)));
    }

    private HttpResponse signTokenHandler(Map<String, Object> params) {
        var subject = params.get("subject").toString();
        var accessToken = params.get("token").toString();
        var audience = params.get("audience").toString();
        var issuer = params.get("issuer").toString();

        var claims = Map.of(
                ISSUER, issuer,
                SUBJECT, subject,
                AUDIENCE, audience,
                PRESENTATION_TOKEN_CLAIM, accessToken);

        var sts = secureTokenServices.get(issuer);
        var token = sts.createToken(issuer, claims, null)
                .map(TokenRepresentation::getToken)
                .orElseThrow(failure -> new RuntimeException(failure.getFailureDetail()));

        return HttpResponse.response(MAPPER.writeValueAsString(Map.of("jwt", token)));
    }
}
