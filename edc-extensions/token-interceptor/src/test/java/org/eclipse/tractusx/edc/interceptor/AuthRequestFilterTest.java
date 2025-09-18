/********************************************************************************
 * Copyright (c) 2025 Cofinity-X GmbH
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

package org.eclipse.tractusx.edc.interceptor;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.V_2025_1_PATH;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthRequestFilterTest {

    private final ContainerRequestContext requestContext = mock();
    private final UriInfo uriInfo = mock();

    private AuthRequestFilter filter;
    private MultivaluedHashMap<String, String> headers;

    @BeforeEach
    void setUp() {
        filter = new AuthRequestFilter();
        headers = new MultivaluedHashMap<>();
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(requestContext.getHeaders()).thenReturn(headers);
    }

    @Test
    void filter_whenProtocolPathAndNonBearerToken_shouldAddBearerPrefix() {
        headers.add(HttpHeaders.AUTHORIZATION, "token123");
        when(uriInfo.getBaseUri()).thenReturn(URI.create("http://example.com/protocol/test"));
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("token123");

        filter.filter(requestContext);

        verify(requestContext).getHeaders();
        assert headers.get(HttpHeaders.AUTHORIZATION).equals(List.of("Bearer token123"));
    }

    @Test
    void filter_whenProtocolPathAndBearerToken_shouldNotModifyHeader() {
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer token123");
        when(uriInfo.getBaseUri()).thenReturn(URI.create("http://example.com/protocol/test"));
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer token123");

        filter.filter(requestContext);

        verify(requestContext, never()).getHeaders();
        assert headers.get(HttpHeaders.AUTHORIZATION).equals(List.of("Bearer token123"));
    }

    @Test
    void filter_whenNoAuthHeader_shouldNotModifyHeaders() {
        when(uriInfo.getBaseUri()).thenReturn(URI.create("http://example.com/protocol/test"));
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(null);

        filter.filter(requestContext);

        verify(requestContext, never()).getHeaders();
    }

    @Test
    void filter_whenNonProtocolPath_shouldNotModifyHeaders() {
        headers.add(HttpHeaders.AUTHORIZATION, "token123");
        when(uriInfo.getBaseUri()).thenReturn(URI.create("http://example.com/api/test"));
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("token123");

        filter.filter(requestContext);

        verify(requestContext, never()).getHeaders();
        assert headers.get(HttpHeaders.AUTHORIZATION).equals(List.of("token123"));
    }

    @Test
    void filter_when2025VersionPath_shouldNotModifyHeaders() {
        headers.add(HttpHeaders.AUTHORIZATION, "token123");
        when(uriInfo.getBaseUri()).thenReturn(URI.create("http://example.com/protocol" + V_2025_1_PATH + "/test"));
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("token123");

        filter.filter(requestContext);

        verify(requestContext, never()).getHeaders();
        assert headers.get(HttpHeaders.AUTHORIZATION).equals(List.of("token123"));
    }
}