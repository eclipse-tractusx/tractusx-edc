/**
 * Copyright (c) 2022 Amadeus - initial API and implementation
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - allow multiple identical query params
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
 **/

package org.eclipse.tractusx.edc.dataplane.proxy.api.controller;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.spi.EdcException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This class provides a set of API wrapping a {@link ContainerRequestContext}.
 */
public class ContainerRequestContextApiImpl implements ContainerRequestContextApi {

    private static final String QUERY_PARAM_SEPARATOR = "&";

    private final ContainerRequestContext context;

    public ContainerRequestContextApiImpl(ContainerRequestContext context) {
        this.context = context;
    }

    @Override
    public Map<String, String> headers() {
        return context.getHeaders().entrySet()
                .stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get(0)));
    }

    @Override
    public String queryParams() {
        return context.getUriInfo().getQueryParameters().entrySet()
                .stream()
                .flatMap(entry -> entry.getValue().stream().map(val -> new QueryParam(entry.getKey(), val)))
                .map(QueryParam::toString)
                .collect(Collectors.joining(QUERY_PARAM_SEPARATOR));
    }

    @Override
    public String body() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(context.getEntityStream()))) {
            return br.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new EdcException("Failed to read request body: " + e.getMessage());
        }
    }

    @Override
    public String mediaType() {
        return Optional.ofNullable(context.getMediaType())
                .map(MediaType::toString)
                .orElse(null);
    }

    @Override
    public String path() {
        var pathInfo = context.getUriInfo().getPath();
        return pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
    }

    @Override
    public String method() {
        return context.getMethod();
    }

    private static final class QueryParam {

        private final String key;
        private final String values;
        private final boolean valid;

        private QueryParam(String key, String values) {
            this.key = key;
            this.values = values;
            this.valid = key != null && values != null && !values.isEmpty();
        }

        public boolean isValid() {
            return valid;
        }

        @Override
        public String toString() {
            return valid ? key + "=" + values : "";
        }
    }
}
