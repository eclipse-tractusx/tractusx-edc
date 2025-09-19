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
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;
import org.eclipse.edc.protocol.dsp.catalog.http.api.CatalogApiPaths;
import org.eclipse.edc.protocol.dsp.negotiation.http.api.NegotiationApiPaths;
import org.eclipse.edc.protocol.dsp.transferprocess.http.api.TransferProcessApiPaths;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;
import static org.eclipse.edc.protocol.dsp.catalog.http.api.CatalogApiPaths.CATALOG_REQUEST;
import static org.eclipse.edc.protocol.dsp.catalog.http.api.CatalogApiPaths.DATASET_REQUEST;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.V_2025_1_PATH;

public class OkHttpInterceptor implements Interceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final List<String> DSP_PATHS = List.of(
            CatalogApiPaths.BASE_PATH + CATALOG_REQUEST,
            CatalogApiPaths.BASE_PATH + DATASET_REQUEST,
            NegotiationApiPaths.BASE_PATH,
            TransferProcessApiPaths.BASE_PATH,
            "/.well-known/dspace-version");

    @Override
    public @NotNull Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        String url = originalRequest.url().toString();

        if (DSP_PATHS.stream().anyMatch(url::contains) && !url.contains(V_2025_1_PATH)) {
            String authHeader = originalRequest.header(HttpHeaders.AUTHORIZATION);
            if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
                String strippedAuth = authHeader.substring(BEARER_PREFIX.length()).trim();
                var body = originalRequest.body();
                if (body != null) {
                    MediaType contentType = body.contentType();
                    byte[] bytes = readBodyBytes(body);
                    Charset charset = resolveCharset(contentType);

                    String bodyString = new String(bytes, charset);
                    RequestBody rebuiltBody = RequestBody.create(bytes, contentType);
                    Request modifiedRequest;

                    if (bodyString.contains(DSPACE_SCHEMA)) {
                        modifiedRequest = originalRequest.newBuilder()
                                .header(HttpHeaders.AUTHORIZATION, strippedAuth)
                                .method(originalRequest.method(), rebuiltBody)
                                .build();

                        return chain.proceed(modifiedRequest);
                    }

                    modifiedRequest = originalRequest.newBuilder()
                            .method(originalRequest.method(), rebuiltBody)
                            .build();

                    return chain.proceed(modifiedRequest);
                }
                Request modifiedRequest = originalRequest.newBuilder()
                        .header(HttpHeaders.AUTHORIZATION, strippedAuth)
                        .build();

                return chain.proceed(modifiedRequest);
            }
        }
        return chain.proceed(originalRequest);
    }

    private static byte[] readBodyBytes(RequestBody body) throws IOException {
        Buffer buffer = new Buffer();
        body.writeTo(buffer);
        return buffer.readByteArray();
    }

    private static Charset resolveCharset(MediaType contentType) {
        Charset charSet = (contentType != null) ? contentType.charset() : null;
        return (charSet != null) ? charSet : StandardCharsets.UTF_8;
    }
}
