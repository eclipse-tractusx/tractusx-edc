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

import jakarta.ws.rs.core.HttpHeaders;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.V_2025_1_PATH;

public class OkHttpInterceptor implements Interceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final List<String> SKIP_PATHS = List.of(
            "/sts/token",
            "/dataflows/check",
            "/presentations/query");

    @Override
    public @NotNull Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        String url = originalRequest.url().toString();

        if (SKIP_PATHS.stream().noneMatch(url::contains) && !url.contains(V_2025_1_PATH)) {
            String authHeader = originalRequest.header(HttpHeaders.AUTHORIZATION);
            if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
                String strippedAuth = authHeader.substring(BEARER_PREFIX.length()).trim();

                Request modifiedRequest = originalRequest.newBuilder()
                        .header(HttpHeaders.AUTHORIZATION, strippedAuth)
                        .build();

                return chain.proceed(modifiedRequest);
            }
        }
        return chain.proceed(originalRequest);
    }
}
