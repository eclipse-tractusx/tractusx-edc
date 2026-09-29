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

package org.eclipse.tractusx.edc.dataplane.proxy.consumer.api;

import org.eclipse.edc.api.auth.spi.AuthenticationService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.web.spi.exception.AuthenticationFailedException;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * This is a carbon-copy of the upstream EDC org.eclipse.edc.api.auth.token.TokenBasedAuthenticationService.
 *
 * @deprecated this is a shim that we need to use until upstream EDC has made the TokenBasedAuthenticationService available for standalone use.
 */
@Deprecated(since = "0.7.2")
public class TokenBasedAuthenticationService implements AuthenticationService {

    public static final String TEMPORARY_USE_WARNING = "The '%s' is only here temporarily and should be replaced with the upstream variant once that is available for standalone use!".formatted(TokenBasedAuthenticationService.class);
    private static final String API_KEY_HEADER_NAME = "x-api-key";
    private final Monitor monitor;
    private final String hardCodedApiKey; //todo: have a list of API keys?

    public TokenBasedAuthenticationService(Monitor monitor, String hardCodedApiKey) {
        this.monitor = monitor.withPrefix(getClass().getSimpleName());
        this.hardCodedApiKey = hardCodedApiKey;
        monitor.warning(TEMPORARY_USE_WARNING);
    }

    /**
     * Checks whether a particular request is authorized based on the "X-Api-Key" header.
     *
     * @param headers The headers, that have to contain the "X-Api-Key" header.
     * @throws IllegalArgumentException The map of headers did not contain the "X-Api-Key" header
     */
    @Override
    public boolean isAuthenticated(Map<String, List<String>> headers) {
        monitor.warning(TEMPORARY_USE_WARNING);

        Objects.requireNonNull(headers, "headers");

        var apiKey = headers.keySet().stream()
                .filter(k -> k.equalsIgnoreCase(API_KEY_HEADER_NAME))
                .map(headers::get)
                .findFirst();

        return apiKey.map(this::checkApiKeyValid).orElseThrow(() -> new AuthenticationFailedException(API_KEY_HEADER_NAME + " not found"));
    }

    private boolean checkApiKeyValid(List<String> apiKeys) {
        return apiKeys.size() == 1 && apiKeys.stream().allMatch(hardCodedApiKey::equalsIgnoreCase);
    }
}
