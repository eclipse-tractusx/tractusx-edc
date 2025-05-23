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

package org.eclipse.tractusx.edc.iam.dcp.sts.dim;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.tractusx.edc.iam.dcp.sts.dim.oauth.DimOauth2Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.http.client.testfixtures.HttpTestUtils.testHttpClient;
import static org.eclipse.edc.iam.identitytrust.spi.SelfIssuedTokenConstants.PRESENTATION_TOKEN_CLAIM;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUER;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SUBJECT;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DimSecureTokenServiceTest {

    static final String DIM_URL = "http://localhost:8080/iatp";
    private final Monitor monitor = mock(Monitor.class);
    private final DimOauth2Client oauth2Client = mock(DimOauth2Client.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final Interceptor interceptor = mock(Interceptor.class);
    private DimSecureTokenService client;

    @BeforeEach
    void setup() {
        client = new DimSecureTokenService(testHttpClient(interceptor), DIM_URL, oauth2Client, mapper, monitor);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    void createToken_grantAccess() throws IOException {
        var response = Map.of("jwt", "responseToken");
        Consumer<Request> requestAcceptor = (request) -> {

            var expectedBody = Map.of(
                    "consumerDid", "issuer",
                    "scope", "read",
                    "credentialTypes", List.of("TestCredential"),
                    "providerDid", "audience");
            var body = getBody(request, new TypeReference<Map<String, Object>>() {
            });

            assertThat(body).extractingByKey("grantAccess").satisfies(payload -> {
                assertThat(((Map) payload)).containsAllEntriesOf(expectedBody);
            });

            assertThat(request.url().url().toString()).isEqualTo(DIM_URL);

        };

        when(oauth2Client.obtainRequestToken()).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("token").build()));
        when(interceptor.intercept(isA(Interceptor.Chain.class)))
                .thenAnswer(invocation -> createResponse(200, invocation, requestAcceptor, response));

        var input = Map.<String, Object>of(ISSUER, "issuer", AUDIENCE, "audience");
        var result = client.createToken(input, "namespace:TestCredential:read");


        assertThat(result).isSucceeded()
                .extracting(TokenRepresentation::getToken)
                .isEqualTo("responseToken");
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    void createToken_signToken() throws IOException {
        var response = Map.of("jwt", "responseToken");
        Consumer<Request> requestAcceptor = (request) -> {
            var expectedBody = Map.of(
                    "issuer", "issuer",
                    "audience", "audience",
                    "subject", "issuer",
                    "token", "accessToken");
            var body = getBody(request, new TypeReference<Map<String, Object>>() {
            });

            assertThat(body).extractingByKey("signToken").satisfies(payload -> {
                assertThat(((Map) payload)).containsAllEntriesOf(expectedBody);
            });

            assertThat(request.url().url().toString()).isEqualTo(DIM_URL);

        };

        when(oauth2Client.obtainRequestToken()).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("token").build()));
        when(interceptor.intercept(isA(Interceptor.Chain.class)))
                .thenAnswer(invocation -> createResponse(200, invocation, requestAcceptor, response));

        var input = Map.<String, Object>of(ISSUER, "issuer", SUBJECT, "issuer", AUDIENCE, "audience", PRESENTATION_TOKEN_CLAIM, "accessToken");
        var result = client.createToken(input, null);

        assertThat(result).isSucceeded()
                .extracting(TokenRepresentation::getToken)
                .isEqualTo("responseToken");
    }

    @Test
    void createToken_grantFails_withDimFailure() throws IOException {

        when(oauth2Client.obtainRequestToken()).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("token").build()));
        when(interceptor.intercept(isA(Interceptor.Chain.class)))
                .thenAnswer(invocation -> createResponse(500, invocation));

        var input = Map.<String, Object>of(ISSUER, "issuer", AUDIENCE, "audience");
        var result = client.createToken(input, "namespace:TestCredential:read");

        assertThat(result).isFailed()
                .satisfies(failure -> assertThat(failure.getFailureDetail()).contains("grantAccess"));

    }

    @Test
    void createToken_grantFails_whenClaimsMissing() throws IOException {

        when(oauth2Client.obtainRequestToken()).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("token").build()));
        when(interceptor.intercept(isA(Interceptor.Chain.class)))
                .thenAnswer(invocation -> createResponse(500, invocation));

        var input = Map.<String, Object>of("foo", "bar");
        var result = client.createToken(input, "namespace:TestCredential:read");

        assertThat(result).isFailed()
                .satisfies(failure -> assertThat(failure.getFailureDetail())
                        .contains("Key iss not found")
                        .contains("Key aud not found"));

    }

    @Test
    void createToken_grantFails_whenBadResponsePayload() throws IOException {

        when(oauth2Client.obtainRequestToken()).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("token").build()));
        when(interceptor.intercept(isA(Interceptor.Chain.class)))
                .thenAnswer(invocation -> createResponse(200, invocation, Map.of()));

        var input = Map.<String, Object>of(ISSUER, "issuer", AUDIENCE, "audience");
        var result = client.createToken(input, "namespace:TestCredential:read");

        assertThat(result).isFailed()
                .satisfies(failure -> assertThat(failure.getFailureDetail())
                        .contains("[grantAccess] Failed to get jwt field"));

    }

    @Test
    void createToken_grantFails_whenInvalidScope() throws IOException {

        when(oauth2Client.obtainRequestToken()).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("token").build()));
        when(interceptor.intercept(isA(Interceptor.Chain.class)))
                .thenAnswer(invocation -> createResponse(500, invocation));

        var input = Map.<String, Object>of(ISSUER, "issuer", AUDIENCE, "audience");
        var result = client.createToken(input, "invalidScope");

        assertThat(result).isFailed()
                .satisfies(failure -> assertThat(failure.getFailureDetail())
                        .contains("Scope string invalidScope has invalid format"));

    }

    @Test
    void createToken_signFails_withDimFailure() throws IOException {

        when(oauth2Client.obtainRequestToken()).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("token").build()));
        when(interceptor.intercept(isA(Interceptor.Chain.class)))
                .thenAnswer(invocation -> createResponse(500, invocation));

        var input = Map.<String, Object>of(ISSUER, "issuer", SUBJECT, "issuer", AUDIENCE, "audience", PRESENTATION_TOKEN_CLAIM, "token");
        var result = client.createToken(input, null);

        assertThat(result).isFailed()
                .satisfies(failure -> assertThat(failure.getFailureDetail()).contains("signToken"));

    }

    @Test
    void createToken_signFails_whenBadResponsePayload() throws IOException {

        when(oauth2Client.obtainRequestToken()).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("token").build()));
        when(interceptor.intercept(isA(Interceptor.Chain.class)))
                .thenAnswer(invocation -> createResponse(200, invocation, Map.of()));

        var input = Map.<String, Object>of(ISSUER, "issuer", SUBJECT, "issuer", AUDIENCE, "audience", PRESENTATION_TOKEN_CLAIM, "token");
        var result = client.createToken(input, null);

        assertThat(result).isFailed()
                .satisfies(failure -> assertThat(failure.getFailureDetail())
                        .contains("[signToken] Failed to get jwt field"));

    }

    @Test
    void createToken_signFails_whenClaimsMissing() throws IOException {

        when(oauth2Client.obtainRequestToken()).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("token").build()));
        when(interceptor.intercept(isA(Interceptor.Chain.class)))
                .thenAnswer(invocation -> createResponse(500, invocation));

        var input = Map.<String, Object>of("foo", "bar");
        var result = client.createToken(input, null);

        assertThat(result).isFailed()
                .satisfies(failure -> assertThat(failure.getFailureDetail())
                        .contains("Key iss not found")
                        .contains("Key sub not found")
                        .contains("Key token not found")
                        .contains("Key aud not found"));

    }

    private Response createResponse(int code, InvocationOnMock invocation) {
        return createResponse(code, invocation, (req) -> {
        }, null);
    }

    private Response createResponse(int code, InvocationOnMock invocation, Object body) {
        return createResponse(code, invocation, (req) -> {
        }, body);
    }


    private Response createResponse(int code, InvocationOnMock invocation, Consumer<Request> consumer, Object body) {
        var bodyString = Optional.ofNullable(body).map(this::toJson).orElse("");
        var request = getRequest(invocation);
        consumer.accept(request);
        return new Response.Builder()
                .protocol(Protocol.HTTP_1_1)
                .request(request)
                .code(code)
                .message("")
                .body(ResponseBody.create(bodyString, MediaType.parse("application/json")))
                .build();
    }

    private String toJson(Object body) {
        try {
            return mapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> T getBody(Request request, TypeReference<T> typeReference) {
        try (var buffer = new Buffer()) {
            Objects.requireNonNull(request.body()).writeTo(buffer);
            return mapper.readValue(buffer.inputStream(), typeReference);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Request getRequest(InvocationOnMock invocation) {
        return invocation.getArgument(0, Interceptor.Chain.class).request();
    }
}
