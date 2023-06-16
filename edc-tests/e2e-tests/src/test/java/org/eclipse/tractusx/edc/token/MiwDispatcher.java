/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.token;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.eclipse.edc.spi.types.TypeManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static java.lang.String.format;

public class MiwDispatcher extends Dispatcher {

    private static final TypeManager MAPPER = new TypeManager();
    
    private final String audience;

    private final Map<String, Object> summaryVc;

    public MiwDispatcher(String bpn, String vcFile, String audience) {
        this.audience = audience;
        var json = format(readVcContent(vcFile), bpn);
        summaryVc = MAPPER.readValue(json, new TypeReference<>() {
        });
    }

    @NotNull
    @Override
    public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) throws InterruptedException {
        return switch (recordedRequest.getPath().split("\\?")[0]) {
            case "/api/credentials" -> credentialResponse();
            case "/api/presentations" -> presentationResponse();
            case "/api/presentations/validation" -> presentationValidationResponse();
            default -> new MockResponse().setResponseCode(404);
        };
    }

    private String readVcContent(String vcFile) {
        var classloader = Thread.currentThread().getContextClassLoader();

        try (var jsonStream = classloader.getResourceAsStream(vcFile)) {
            Objects.requireNonNull(jsonStream);
            return new String(jsonStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private MockResponse credentialResponse() {
        return new MockResponse().setBody(MAPPER.writeValueAsString(Map.of("content", List.of(summaryVc))));
    }

    private MockResponse presentationResponse() {
        try {
            var jwt = createJwt(UUID.randomUUID().toString(), createClaims(Instant.now(), createVerifiablePresentationClaim()), testKey().toPrivateKey());
            return new MockResponse().setBody(MAPPER.writeValueAsString(Map.of("vp", jwt)));
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Object> createVerifiablePresentationClaim() {
        var ctx = List.of("https://www.w3.org/2018/credentials/v1");
        var type = List.of("VerifiablePresentation");
        return Map.of(
                "@context", ctx,
                "type", type,
                "verifiableCredential", List.of(summaryVc));
    }

    private MockResponse presentationValidationResponse() {
        return new MockResponse().setBody(MAPPER.writeValueAsString(Map.of("valid", true)));
    }

    private JWTClaimsSet createClaims(Instant exp, Map<String, Object> presentation) {
        return new JWTClaimsSet.Builder()
                .claim("vp", presentation)
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
