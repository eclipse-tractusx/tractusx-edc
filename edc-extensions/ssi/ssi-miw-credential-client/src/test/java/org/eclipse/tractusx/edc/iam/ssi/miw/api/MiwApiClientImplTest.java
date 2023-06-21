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

package org.eclipse.tractusx.edc.iam.ssi.miw.api;

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
import org.eclipse.tractusx.edc.iam.ssi.miw.oauth2.MiwOauth2Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.testfixtures.TestUtils.testHttpClient;
import static org.eclipse.tractusx.edc.iam.ssi.miw.api.MiwApiClientImpl.CREDENTIAL_PATH;
import static org.eclipse.tractusx.edc.iam.ssi.miw.api.MiwApiClientImpl.HOLDER_IDENTIFIER;
import static org.eclipse.tractusx.edc.iam.ssi.miw.api.MiwApiClientImpl.PRESENTATIONS_PATH;
import static org.eclipse.tractusx.edc.iam.ssi.miw.api.MiwApiClientImpl.PRESENTATIONS_VALIDATION_PATH;
import static org.eclipse.tractusx.edc.iam.ssi.miw.api.MiwApiClientImpl.VERIFIABLE_CREDENTIALS;
import static org.eclipse.tractusx.edc.iam.ssi.miw.api.MiwApiClientImpl.VP_FIELD;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MiwApiClientImplTest {

    static final String BASE_URL = "http://localhost:8080";
    Interceptor interceptor = mock(Interceptor.class);
    MiwApiClientImpl client;
    Monitor monitor = mock(Monitor.class);
    MiwOauth2Client oauth2Client = mock(MiwOauth2Client.class);
    ObjectMapper mapper = new ObjectMapper();

    String participantId = "participantId";

    String authorityId = "authorityId";

    @BeforeEach
    void setup() {
        client = new MiwApiClientImpl(testHttpClient(interceptor), BASE_URL, oauth2Client, participantId, authorityId, mapper, monitor);
    }

    @Test
    void getCredentials() throws IOException {

        var credentialType = "test";

        var response = Map.of("content", List.of(Map.of("id", "test")));
        var expectedUrl = format(BASE_URL + CREDENTIAL_PATH + "?issuerIdentifier=%s&type=%s", authorityId, credentialType);

        Consumer<Request> requestAcceptor = (request) -> {
            assertThat(request.url().url().toString()).isEqualTo(expectedUrl);
        };

        when(interceptor.intercept(isA(Interceptor.Chain.class)))
                .thenAnswer(invocation -> createResponse(200, invocation, requestAcceptor, response));

        when(oauth2Client.obtainRequestToken()).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("testToken").build()));

        var result = client.getCredentials(Set.of("test"));

        assertThat(result).isNotNull().matches(Result::succeeded)
                .extracting(Result::getContent)
                .asList().hasSize(1);
    }

    @Test
    void getCredentials_fails_whenMiwFails() throws IOException {

        when(interceptor.intercept(isA(Interceptor.Chain.class)))
                .thenAnswer(invocation -> createResponse(500, invocation));

        when(oauth2Client.obtainRequestToken()).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("testToken").build()));

        var result = client.getCredentials(Set.of("test"));

        assertThat(result).isNotNull().matches(Result::failed);
    }

    @Test
    void getCredentials_fails_whenTokenRequestFails() {

        when(oauth2Client.obtainRequestToken()).thenReturn(Result.failure("Token fetch failure"));

        var result = client.getCredentials(Set.of("test"));

        assertThat(result).isNotNull().matches(Result::failed);
    }

    @Test
    void createPresentation() throws IOException {
        var audience = "audience";
        var response = Map.of("vp", Map.of());
        var expectedUrl = format(BASE_URL + PRESENTATIONS_PATH + "?asJwt=true&audience=%s", audience);

        Consumer<Request> requestAcceptor = (request) -> {
            var expectedBody = Map.of(HOLDER_IDENTIFIER, participantId, VERIFIABLE_CREDENTIALS, List.of());
            var body = getBody(request, new TypeReference<Map<String, Object>>() {
            });

            assertThat(body).containsAllEntriesOf(expectedBody);

            assertThat(request.url().url().toString()).isEqualTo(expectedUrl);
        };

        when(interceptor.intercept(isA(Interceptor.Chain.class)))
                .thenAnswer(invocation -> createResponse(200, invocation, requestAcceptor, response));

        when(oauth2Client.obtainRequestToken()).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("testToken").build()));

        var result = client.createPresentation(List.of(), audience);

        assertThat(result).isNotNull().matches(Result::succeeded);
    }

    @Test
    void createPresentation_fails_whenMiwFails() throws IOException {

        when(interceptor.intercept(isA(Interceptor.Chain.class)))
                .thenAnswer(invocation -> createResponse(500, invocation));

        when(oauth2Client.obtainRequestToken()).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("testToken").build()));

        var result = client.createPresentation(List.of(), "audience");

        assertThat(result).isNotNull().matches(Result::failed);
    }

    @Test
    void createPresentation_fails_whenTokenRequestFails() {

        when(oauth2Client.obtainRequestToken()).thenReturn(Result.failure("Token fetch failure"));

        var result = client.createPresentation(List.of(), "audience");

        assertThat(result).isNotNull().matches(Result::failed);
    }
    
    @Test
    void verifyPresentation() throws IOException {
        var jwt = "jwt";
        var verifyRequest = Map.of(VP_FIELD, jwt);
        var audience = "audience";
        var expectedUrl = format(BASE_URL + PRESENTATIONS_VALIDATION_PATH + "?asJwt=true&audience=%s", audience);

        Consumer<Request> requestAcceptor = (request) -> {

            var body = getBody(request, new TypeReference<Map<String, Object>>() {
            });

            assertThat(body).containsAllEntriesOf(verifyRequest);
            assertThat(request.url().url().toString()).isEqualTo(expectedUrl);
        };
        var verifyResponse = Map.of("valid", true);

        when(interceptor.intercept(isA(Interceptor.Chain.class)))
                .thenAnswer(invocation -> createResponse(200, invocation, requestAcceptor, verifyResponse));

        when(oauth2Client.obtainRequestToken()).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("testToken").build()));

        var result = client.verifyPresentation(jwt, "audience");

        assertThat(result).isNotNull().matches(Result::succeeded);
    }

    @Test
    void verifyPresentation_fails_whenNotVerified() throws IOException {
        var jwt = "jwt";
        var verifyRequest = Map.of(VP_FIELD, jwt);
        var audience = "audience";
        var expectedUrl = format(BASE_URL + PRESENTATIONS_VALIDATION_PATH + "?asJwt=true&audience=%s", audience);

        Consumer<Request> requestAcceptor = (request) -> {

            var body = getBody(request, new TypeReference<Map<String, Object>>() {
            });

            assertThat(body).containsAllEntriesOf(verifyRequest);
            assertThat(request.url().url().toString()).isEqualTo(expectedUrl);
        };
        var verifyResponse = Map.of("valid", false);

        when(interceptor.intercept(isA(Interceptor.Chain.class)))
                .thenAnswer(invocation -> createResponse(200, invocation, requestAcceptor, verifyResponse));

        when(oauth2Client.obtainRequestToken()).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("testToken").build()));

        var result = client.verifyPresentation(jwt, "audience");

        assertThat(result).isNotNull().matches(Result::failed);
    }

    @Test
    void verifyPresentation_fails_whenMiwFails() throws IOException {

        when(interceptor.intercept(isA(Interceptor.Chain.class)))
                .thenAnswer(invocation -> createResponse(500, invocation));

        when(oauth2Client.obtainRequestToken()).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token("testToken").build()));

        var result = client.verifyPresentation("jwt", "audience");

        assertThat(result).isNotNull().matches(Result::failed);
    }

    @Test
    void verifyPresentation_fails_whenTokenRequestFails() throws IOException {

        when(oauth2Client.obtainRequestToken()).thenReturn(Result.failure("Token fetch failure"));

        var result = client.verifyPresentation("jwt", "audience");

        assertThat(result).isNotNull().matches(Result::failed);
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
