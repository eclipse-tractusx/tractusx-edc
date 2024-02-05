/********************************************************************************
 * Copyright (c) 2024 Robert Bosch Manufacturing Solutions GmbH and others
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;
import org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.AccessControlServerException;
import org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.HttpAccessControlCheckClientConfig;
import org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.HttpAccessControlCheckDtrClientConfig;
import org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.client.model.DtrAccessVerificationRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.HttpAccessControlRequestParamsDecorator.HEADER_EDC_BPN;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DtrAccessVerificationClientTest {
    static final String BPN = "BPNL000000000001";
    static final String POST = "POST";
    static final String SCOPE = "aud:dtr";
    static final String DUMMY_TOKEN = "dummy_token_value";
    static final String BEARER_PREFIX = "Bearer ";
    static final String LOCALHOST_ACCESS_VERIFICATION = "https://localhost/access-verification";
    static final String LOCALHOST_EDC_DATA_PROXY = "http://localhost/edc-data/proxy";
    static final String REQUEST_LOCALHOST_EDC_DATA_PROXY = "{\"submodelEndpointUrl\":\"" + LOCALHOST_EDC_DATA_PROXY + "\"}";
    @Mock
    private Response httpResponse;
    @Mock
    private DataFlowRequest request;
    @Mock
    private HttpDataAddress dataAddress;
    @Mock
    private Monitor monitor;
    @Mock
    private EdcHttpClient httpClient;
    @Mock
    private Oauth2TokenClient tokenClient;
    @Mock
    private TypeManager typeManager;
    @Mock
    private HttpAccessControlCheckClientConfig config;
    @Mock
    private HttpAccessControlCheckDtrClientConfig dtrConfig;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    public static Stream<Arguments> urlMatcherSourceProvider() {
        return Stream.<Arguments>builder()
                .add(Arguments.of("^abcd$", "abcd", true))
                .add(Arguments.of("^abcd$", "ABCDabcd0123", false))
                .add(Arguments.of("abcd", "ABCDabcd0123", true))
                .add(Arguments.of("^[a-dA-D]+$", "Abcd", true))
                .build();
    }

    @ParameterizedTest
    @MethodSource("urlMatcherSourceProvider")
    void test_IsAspectModelCall_ShouldReturnTrue_WhenUrlIsMatchingPattern(
            final String pattern, final String url, final boolean expected) {
        //given
        when(dtrConfig.getAspectModelUrlPattern()).thenReturn(pattern);
        final var underTest = new DtrAccessVerificationClient(monitor, httpClient, tokenClient, typeManager, config, dtrConfig);

        //when
        final boolean actual = underTest.isAspectModelCall(url);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @SuppressWarnings("resource")
    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void test_ShouldAllowAccess_ShouldReturnTrue_WhenDtrResponseIsSuccessful(
            final int cacheForMinutes) throws IOException {
        //given
        final Map<String, String> additionalHeaders = Map.of(HEADER_EDC_BPN, BPN);
        final Map<String, String> properties = Map.of();
        when(request.getProperties()).thenReturn(properties);
        when(dtrConfig.getDtrAccessVerificationUrl()).thenReturn(LOCALHOST_ACCESS_VERIFICATION);
        when(dtrConfig.getDecisionCacheDurationMinutes()).thenReturn(cacheForMinutes);
        when(config.getEdcDataPlaneBaseUrl()).thenReturn(LOCALHOST_EDC_DATA_PROXY);
        when(dtrConfig.getOauth2TokenScope()).thenReturn(SCOPE);

        when(tokenClient.getBearerToken(SCOPE)).thenReturn(DUMMY_TOKEN);
        final var requestMatcher = new RequestMatcher(
                LOCALHOST_ACCESS_VERIFICATION, BPN, BEARER_PREFIX + DUMMY_TOKEN, LOCALHOST_EDC_DATA_PROXY);
        when(typeManager.writeValueAsString(any(DtrAccessVerificationRequest.class))).thenReturn(REQUEST_LOCALHOST_EDC_DATA_PROXY);
        when(httpClient.execute(argThat(requestMatcher))).thenReturn(httpResponse);
        when(httpResponse.isSuccessful()).thenReturn(true);
        final var underTest = new DtrAccessVerificationClient(monitor, httpClient, tokenClient, typeManager, config, dtrConfig);

        //when
        final boolean actual = underTest.shouldAllowAccess(additionalHeaders, request, dataAddress);

        //then
        assertThat(actual).isTrue();
        verify(request, atLeastOnce()).getProperties();
        verify(tokenClient).getBearerToken(SCOPE);
        verify(httpClient).execute(argThat(requestMatcher));
    }

    @SuppressWarnings("resource")
    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void test_ShouldAllowAccess_ShouldReturnFalse_WhenDtrResponseIsNotSuccessful(
            final int cacheForMinutes) throws IOException {
        //given
        final Map<String, String> additionalHeaders = Map.of(HEADER_EDC_BPN, BPN);
        final Map<String, String> properties = Map.of();
        when(request.getProperties()).thenReturn(properties);
        when(dtrConfig.getDtrAccessVerificationUrl()).thenReturn(LOCALHOST_ACCESS_VERIFICATION);
        when(dtrConfig.getDecisionCacheDurationMinutes()).thenReturn(cacheForMinutes);
        when(config.getEdcDataPlaneBaseUrl()).thenReturn(LOCALHOST_EDC_DATA_PROXY);
        when(dtrConfig.getOauth2TokenScope()).thenReturn(SCOPE);

        when(tokenClient.getBearerToken(SCOPE)).thenReturn(DUMMY_TOKEN);
        final var requestMatcher = new RequestMatcher(
                LOCALHOST_ACCESS_VERIFICATION, BPN, BEARER_PREFIX + DUMMY_TOKEN, LOCALHOST_EDC_DATA_PROXY);
        when(typeManager.writeValueAsString(any(DtrAccessVerificationRequest.class))).thenReturn(REQUEST_LOCALHOST_EDC_DATA_PROXY);
        when(httpClient.execute(argThat(requestMatcher))).thenReturn(httpResponse);
        when(httpResponse.isSuccessful()).thenReturn(false);
        final var underTest = new DtrAccessVerificationClient(monitor, httpClient, tokenClient, typeManager, config, dtrConfig);

        //when
        final boolean actual = underTest.shouldAllowAccess(additionalHeaders, request, dataAddress);

        //then
        assertThat(actual).isFalse();
        verify(request, atLeastOnce()).getProperties();
        verify(tokenClient).getBearerToken(SCOPE);
        verify(httpClient).execute(argThat(requestMatcher));
    }

    @SuppressWarnings("resource")
    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void test_ShouldDenyAccess_ShouldThrowException_WhenTokenCannotBeObtained(
            final int cacheForMinutes) throws IOException {
        //given
        final Map<String, String> additionalHeaders = Map.of(HEADER_EDC_BPN, BPN);
        final Map<String, String> properties = Map.of();
        when(request.getProperties()).thenReturn(properties);
        when(dtrConfig.getDtrAccessVerificationUrl()).thenReturn(LOCALHOST_ACCESS_VERIFICATION);
        when(dtrConfig.getDecisionCacheDurationMinutes()).thenReturn(cacheForMinutes);
        when(config.getEdcDataPlaneBaseUrl()).thenReturn(LOCALHOST_EDC_DATA_PROXY);
        when(dtrConfig.getOauth2TokenScope()).thenReturn(SCOPE);

        when(tokenClient.getBearerToken(SCOPE)).thenReturn(null);
        final var underTest = new DtrAccessVerificationClient(monitor, httpClient, tokenClient, typeManager, config, dtrConfig);

        //when
        assertThatExceptionOfType(AccessControlServerException.class)
                .isThrownBy(() -> underTest.shouldAllowAccess(additionalHeaders, request, dataAddress));

        //then
        verify(request, atLeastOnce()).getProperties();
        verify(tokenClient).getBearerToken(SCOPE);
        verify(httpClient, never()).execute(any());
    }

    @SuppressWarnings("resource")
    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void test_ShouldDenyAccess_ShouldThrowException_WhenDtrRequestResultsInException(
            final int cacheForMinutes) throws IOException {
        //given
        final Map<String, String> additionalHeaders = Map.of(HEADER_EDC_BPN, BPN);
        final Map<String, String> properties = Map.of();
        when(request.getProperties()).thenReturn(properties);
        when(dtrConfig.getDtrAccessVerificationUrl()).thenReturn(LOCALHOST_ACCESS_VERIFICATION);
        when(dtrConfig.getDecisionCacheDurationMinutes()).thenReturn(cacheForMinutes);
        when(config.getEdcDataPlaneBaseUrl()).thenReturn(LOCALHOST_EDC_DATA_PROXY);
        when(dtrConfig.getOauth2TokenScope()).thenReturn(SCOPE);

        when(tokenClient.getBearerToken(SCOPE)).thenReturn(DUMMY_TOKEN);
        final var requestMatcher = new RequestMatcher(
                LOCALHOST_ACCESS_VERIFICATION, BPN, BEARER_PREFIX + DUMMY_TOKEN, LOCALHOST_EDC_DATA_PROXY);
        when(typeManager.writeValueAsString(any(DtrAccessVerificationRequest.class))).thenReturn(REQUEST_LOCALHOST_EDC_DATA_PROXY);
        when(httpClient.execute(argThat(requestMatcher))).thenThrow(IOException.class);
        final var underTest = new DtrAccessVerificationClient(monitor, httpClient, tokenClient, typeManager, config, dtrConfig);

        //when
        assertThatExceptionOfType(AccessControlServerException.class)
                .isThrownBy(() -> underTest.shouldAllowAccess(additionalHeaders, request, dataAddress));

        //then
        verify(request, atLeastOnce()).getProperties();
        verify(tokenClient).getBearerToken(SCOPE);
        verify(httpClient).execute(any());
        verify(httpResponse, never()).isSuccessful();
    }

    private record RequestMatcher(String url, String bpn, String authorization,
                                  String targetUrl) implements ArgumentMatcher<Request> {
        @SuppressWarnings("DataFlowIssue")
        @Override
        public boolean matches(final Request request) {
            final boolean methodMatched = POST.equals(request.method());
            final boolean urlMatched = url.equals(request.url().url().toString());
            final boolean bpnHeaderFound = bpn.equals(request.header(HEADER_EDC_BPN));
            final boolean authHeaderFound = authorization.equals(request.header(AUTHORIZATION));
            final RequestBody body = request.body();
            final boolean contentTypeHeaderFound = (body.contentType().type() + "/" + body.contentType().subtype()).equals(request.header(CONTENT_TYPE));
            final String submodelEndpointUrl = bodyAsObject(body).submodelEndpointUrl();
            final boolean bodyMatched = targetUrl.equals(submodelEndpointUrl);
            return methodMatched && urlMatched && bpnHeaderFound && authHeaderFound && contentTypeHeaderFound && bodyMatched;
        }

        private DtrAccessVerificationRequest bodyAsObject(RequestBody body) {
            try {
                final Buffer buffer = new Buffer();
                body.writeTo(buffer);
                final String string = buffer.readUtf8();
                return new ObjectMapper().readValue(string, DtrAccessVerificationRequest.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}