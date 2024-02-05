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
import org.eclipse.edc.connector.dataplane.http.spi.HttpParamsDecorator;
import org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParams;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.eclipse.tractusx.edc.dataplane.dtr.http.accesscontrol.client.HttpAccessVerificationClient;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class HttpAccessControlRequestParamsDecorator implements HttpParamsDecorator {
    public static final String HEADER_EDC_BPN = "Edc-Bpn";

    private final Monitor monitor;

    private final HttpAccessControlCheckClientConfig config;

    private final Map<String, HttpAccessVerificationClient> clients;

    public HttpAccessControlRequestParamsDecorator(
            final Monitor monitor,
            final Map<String, HttpAccessVerificationClient> clients,
            final HttpAccessControlCheckClientConfig config) {
        this.monitor = monitor;
        this.clients = clients;
        this.config = config;
    }

    @Override
    public HttpRequestParams.Builder decorate(final DataFlowRequest request, final HttpDataAddress address, final HttpRequestParams.Builder params) {
        final var targetUrl = address.getBaseUrl() + Optional.ofNullable(request.getProperties().get("pathSegments")).orElse("");
        final Map<String, String> additionalHeaders = address.getAdditionalHeaders();
        params.header(HEADER_EDC_BPN, additionalHeaders.getOrDefault(HEADER_EDC_BPN, ""));
        final var relevantClients = clients.values().stream()
                .filter(client -> client.isAspectModelCall(targetUrl))
                .collect(Collectors.toSet());
        if (!relevantClients.isEmpty() && relevantClients.stream().noneMatch(client -> client.shouldAllowAccess(additionalHeaders, request, address))) {
            params.baseUrl(config.getErrorEndpointBaseUrl())
                    .queryParams(config.getReasonPhraseParameterName() + "=Access denied.")
                    .method("GET")
                    .path(config.getErrorEndpointPathForStatus(403));
        }

        return params;
    }
}
