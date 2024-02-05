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

package org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol;

import org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress;
import org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParams;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.client.HttpAccessVerificationClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.HttpAccessControlRequestParamsDecorator.HEADER_EDC_BPN;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class HttpAccessControlRequestParamsDecoratorTest {

    private static final int HTTP_403 = 403;

    final Monitor monitor = mock();
    final DataFlowRequest request = mock();
    final HttpDataAddress address = mock();
    final HttpRequestParams.Builder params = mock();
    final HttpAccessControlCheckClientConfig config = mock();
    final HttpAccessControlCheckDtrClientConfig dtrConfig = mock();
    final HttpAccessVerificationClient client = mock();
    HttpAccessControlRequestParamsDecorator underTest;

    @BeforeEach
    void initMocks() {
        when(request.getId()).thenReturn("ID");

        when(address.getMethod()).thenReturn("GET");
        when(address.getBaseUrl()).thenReturn("http://example.com");
        when(address.getPath()).thenReturn("");
        when(address.getQueryParams()).thenReturn("?param1=value1");

        when(params.header(anyString(), anyString())).thenReturn(params);

        when(dtrConfig.getAspectModelUrlPattern()).thenReturn("http:\\/\\/example\\.com\\/path\\/to.*");
        when(config.getDtrClientConfigMap()).thenReturn(Map.of("0", dtrConfig));
        underTest = new HttpAccessControlRequestParamsDecorator(monitor, Map.of("0", client), config);
    }

    @Test
    void test_DtrLookupCall_ShouldSucceed_WhenBpnHeaderIsPresent() {
        //given
        final Map<String, String> map = Map.of("pathSegments", "/path/dtr/resource");
        when(request.getProperties()).thenReturn(map);
        final Map<String, String> additionalHeaders = Map.of(
                HEADER_EDC_BPN, "BPN0001"
        );
        when(address.getAdditionalHeaders()).thenReturn(additionalHeaders);
        when(client.isAspectModelCall(anyString())).thenReturn(false);

        //when
        final HttpRequestParams.Builder actual = underTest.decorate(request, address, params);

        //then
        verify(params).header(HEADER_EDC_BPN, "BPN0001");
        assertThat(actual).isSameAs(params);
    }

    @Test
    void test_DtrLookupCall_ShouldSucceed_WhenBpnHeaderIsMissing() {
        //given
        final Map<String, String> map = Map.of("pathSegments", "/path/dtr/resource");
        when(request.getProperties()).thenReturn(map);
        final Map<String, String> additionalHeaders = Map.of();
        when(address.getAdditionalHeaders()).thenReturn(additionalHeaders);
        when(client.isAspectModelCall(anyString())).thenReturn(false);

        //when
        final HttpRequestParams.Builder actual = underTest.decorate(request, address, params);

        //then
        verify(params).header(HEADER_EDC_BPN, "");
        assertThat(actual).isSameAs(params);
    }

    @Test
    void test_AspectModelBackendRequest_ShouldSucceed_WhenBpnHeaderIsPresentAndDtrRespondsWithOk() {
        //given
        final Map<String, String> map = Map.of("pathSegments", "/path/that/does/not/match");
        when(request.getProperties()).thenReturn(map);
        final Map<String, String> additionalHeaders = Map.of(
                HEADER_EDC_BPN, "BPN0001"
        );
        when(address.getAdditionalHeaders()).thenReturn(additionalHeaders);
        when(client.isAspectModelCall(anyString())).thenReturn(true);
        when(client.shouldAllowAccess(additionalHeaders, request, address)).thenReturn(true);

        //when
        final HttpRequestParams.Builder actual = underTest.decorate(request, address, params);

        //then
        verify(monitor, never()).info(anyString());
        verify(params).header(HEADER_EDC_BPN, "BPN0001");
        verify(client).shouldAllowAccess(anyMap(), any(), any());
        assertThat(actual).isSameAs(params);
    }

    @Test
    void test_AspectModelBackendRequest_ShouldThrowException_WhenBpnHeaderIsPresentAndDtrRespondsWithError() {
        //given
        final Map<String, String> map = Map.of("pathSegments", "/path/that/does/not/match");
        when(request.getProperties()).thenReturn(map);
        final Map<String, String> additionalHeaders = Map.of(
                HEADER_EDC_BPN, "BPN0001"
        );
        when(address.getAdditionalHeaders()).thenReturn(additionalHeaders);
        when(client.isAspectModelCall(anyString())).thenReturn(true);
        when(client.shouldAllowAccess(anyMap(), any(DataFlowRequest.class), any(HttpDataAddress.class)))
                .thenReturn(false);

        //when
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> underTest.decorate(request, address, params));

        //then
        verify(monitor, never()).info(anyString());
        verify(params).header(HEADER_EDC_BPN, "BPN0001");
        verify(client).shouldAllowAccess(anyMap(), any(), any());
    }

    @Test
    void test_AspectModelBackendRequest_ShouldThrowException_WhenBpnHeaderIsMissing() {
        //given
        final Map<String, String> map = Map.of("pathSegments", "/path/that/does/not/match");
        when(request.getProperties()).thenReturn(map);
        final Map<String, String> additionalHeaders = Map.of();
        when(address.getAdditionalHeaders()).thenReturn(additionalHeaders);
        when(client.isAspectModelCall(anyString())).thenReturn(true);
        when(client.shouldAllowAccess(anyMap(), any(DataFlowRequest.class), any(HttpDataAddress.class)))
                .thenReturn(false);

        //when
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> underTest.decorate(request, address, params));

        //then
        verify(monitor, never()).info(anyString());
        verify(params).header(HEADER_EDC_BPN, "");
        verify(client).shouldAllowAccess(anyMap(), any(), any());
    }

    @Test
    void test_AspectModelBackendRequest_ShouldRedirectToErrorPage_WhenAccessIsDenied() {
        //given
        final Map<String, String> map = Map.of("pathSegments", "/path/that/does/not/match", "queryParams", "id=1");
        when(request.getProperties()).thenReturn(map);
        final Map<String, String> additionalHeaders = Map.of(
                HEADER_EDC_BPN, "BPN0001"
        );
        final String errorPageBaseUrl = "http://localhost:9090/error";
        final String reasonPhraseParam = "reasonPhrase";
        final String errorPagePath = "/http/access/error/403";
        final HttpRequestParams.Builder builder = HttpRequestParams.Builder.newInstance();
        when(config.getErrorEndpointBaseUrl()).thenReturn(errorPageBaseUrl);
        when(config.getReasonPhraseParameterName()).thenReturn(reasonPhraseParam);
        when(config.getErrorEndpointPathForStatus(HTTP_403)).thenReturn(errorPagePath);

        when(client.isAspectModelCall(anyString())).thenReturn(true);
        when(address.getAdditionalHeaders()).thenReturn(additionalHeaders);
        when(client.shouldAllowAccess(anyMap(), any(DataFlowRequest.class), any(HttpDataAddress.class)))
                .thenReturn(false);

        //when
        final HttpRequestParams actual = underTest.decorate(request, address, builder).build();

        //then
        assertThat(actual).isNotNull();
        assertThat(actual.getBaseUrl()).isEqualTo(errorPageBaseUrl);
        assertThat(actual.getPath()).isEqualTo(errorPagePath);
        assertThat(actual.getQueryParams()).startsWith(reasonPhraseParam);
        assertThat(actual.getHeaders()).containsEntry(HEADER_EDC_BPN, "BPN0001");
        verify(monitor, never()).info(anyString());
        verify(client).shouldAllowAccess(anyMap(), any(), any());
    }
}