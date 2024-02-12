/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.iam.ssi.miw.credentials;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.json.Json;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.tractusx.edc.iam.ssi.miw.api.MiwApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SCOPE;
import static org.eclipse.tractusx.edc.iam.ssi.miw.api.MiwApiClient.VP;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class SsiMiwCredentialClientTest {

    private final String audience = "audience";
    SsiMiwCredentialClient credentialClient;
    MiwApiClient apiClient = mock(MiwApiClient.class);
    JsonLd jsonLdService = mock(JsonLd.class);

    Monitor monitor = mock(Monitor.class);
    private RSAKey key;

    @BeforeEach
    void setup() throws JOSEException {
        credentialClient = new SsiMiwCredentialClient(apiClient, jsonLdService, monitor);
        key = testKey();
    }

    @Test
    void validate_success() throws JOSEException {
        var claims = createClaims(Instant.now());
        var jwt = createJwt(UUID.randomUUID().toString(), claims, key.toPrivateKey());
        when(apiClient.verifyPresentation(jwt, audience)).thenReturn(Result.success());
        when(jsonLdService.expand(any())).thenReturn(Result.success(Json.createObjectBuilder().build()));

        var result = credentialClient.validate(TokenRepresentation.Builder.newInstance().token(jwt).build());

        assertThat(result).isNotNull().matches(Result::succeeded);
        verify(apiClient).verifyPresentation(jwt, audience);
    }

    @Test
    void validate_success_whenClientFails() throws JOSEException {
        var claims = createClaims(Instant.now());
        var jwt = createJwt(UUID.randomUUID().toString(), claims, key.toPrivateKey());
        when(apiClient.verifyPresentation(jwt, audience)).thenReturn(Result.failure("fail"));
        when(jsonLdService.expand(any())).thenReturn(Result.success(Json.createObjectBuilder().build()));

        var result = credentialClient.validate(TokenRepresentation.Builder.newInstance().token(jwt).build());

        assertThat(result).isNotNull().matches(Result::failed);
        verify(apiClient).verifyPresentation(jwt, audience);
    }

    @Test
    void validate_fail_whenInvalidToken() throws JOSEException {

        var result = credentialClient.validate(TokenRepresentation.Builder.newInstance().token("invalid").build());

        assertThat(result).isNotNull().matches(Result::failed);
        verifyNoInteractions(apiClient);
    }

    @Test
    void obtainCredentials_success() {

        var jwt = "serialized";
        Map<String, Object> credential = Map.of();
        Map<String, Object> presentation = Map.of(VP, jwt);

        var credentials = List.of(credential);

        when(apiClient.getCredentials(Set.of())).thenReturn(Result.success(credentials));
        when(apiClient.createPresentation(credentials, audience)).thenReturn(Result.success(presentation));
        var result = credentialClient.obtainClientCredentials(TokenParameters.Builder.newInstance().claims(AUDIENCE, audience).claims(SCOPE, "").build());

        assertThat(result).isNotNull()
                .extracting(Result::getContent)
                .extracting(TokenRepresentation::getToken)
                .isEqualTo(jwt);

        verify(apiClient).getCredentials(Set.of());
    }

    private JWTClaimsSet createClaims(Instant exp) {
        return new JWTClaimsSet.Builder()
                .claim("foo", "bar")
                .claim(VP, Map.of())
                .audience(audience)
                .expirationTime(Date.from(exp))
                .build();
    }

    private String createJwt(String publicKeyId, JWTClaimsSet claimsSet, PrivateKey pk) {
        var header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(publicKeyId).build();
        try {
            SignedJWT jwt = new SignedJWT(header, claimsSet);
            jwt.sign(new RSASSASigner(pk));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new AssertionError(e);
        }
    }

    private RSAKey testKey() throws JOSEException {
        return new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE) // indicate the intended use of the key
                .keyID(UUID.randomUUID().toString()) // give the key a unique ID
                .generate();
    }
}
