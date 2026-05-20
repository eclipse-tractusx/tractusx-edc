/*
 * Copyright (c) 2026 Think-it GmbH
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

package org.eclipse.tractusx.edc.jsonld;

import org.eclipse.edc.jsonld.spi.JsonLdContext;
import org.eclipse.edc.spi.result.Result;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.EDC_CONTEXT;
import static org.eclipse.tractusx.edc.jsonld.JsonLdExtension.TX_AUTH_CONTEXT;

public class TxCachedDocumentRegistry {

    public static final String CREDENTIALS_V_1 = "https://www.w3.org/2018/credentials/v1";
    public static final String SECURITY_JWS_V1 = "https://w3id.org/security/suites/jws-2020/v1";
    public static final String SECURITY_ED25519_V1 = "https://w3id.org/security/suites/ed25519-2020/v1";

    public static Stream<Result<JsonLdContext>> getDocuments() {
        return Map.of(
                        "credential-v1.jsonld", CREDENTIALS_V_1,
                        "security-jws-2020.jsonld", SECURITY_JWS_V1,
                        "security-ed25519-2020.jsonld", SECURITY_ED25519_V1,
                        "tx-auth-v1.jsonld", TX_AUTH_CONTEXT,
                        "edc-v1.jsonld", EDC_CONTEXT
                ).entrySet().stream()
                .map(entry -> getResourceUri("document/" + entry.getKey())
                        .map(uri -> new JsonLdContext(uri, entry.getValue())));
    }

    static Result<URI> getResourceUri(String name) {
        var uri = TxCachedDocumentRegistry.class.getClassLoader().getResource(name);
        if (uri == null) {
            return Result.failure(format("Cannot find resource %s", name));
        }

        try {
            return Result.success(uri.toURI());
        } catch (URISyntaxException e) {
            return Result.failure(format("Cannot read resource %s: %s", name, e.getMessage()));
        }
    }
}
