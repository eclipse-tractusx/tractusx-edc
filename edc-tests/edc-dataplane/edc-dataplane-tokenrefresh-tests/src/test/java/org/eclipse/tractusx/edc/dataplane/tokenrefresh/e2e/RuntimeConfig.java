/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.dataplane.tokenrefresh.e2e;

import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.connector.controlplane.test.system.utils.LazySupplier;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;

import java.net.URI;
import java.util.HashMap;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.eclipse.edc.util.io.Ports.getFreePort;

/**
 * Configuration baseline for Data-Plane e2e tests
 */
public class RuntimeConfig {

    private final LazySupplier<URI> publicApi = new LazySupplier<>(() -> URI.create("http://localhost:%d/public".formatted(getFreePort())));

    public Config getConfig() {
        var settings = new HashMap<String, String>() {
            {
                put("web.http.path", "/api");
                put("web.http.port", String.valueOf(getFreePort()));
                put("web.http.public.path", publicApi.get().getPath());
                put("web.http.public.port", String.valueOf(publicApi.get().getPort()));
                put("web.http.control.path", "/control");
                put("web.http.control.port", String.valueOf(getFreePort()));
                put("edc.dpf.selector.url", "http://not-used/feature");
                put("edc.iam.issuer.id", "did:web:" + UUID.randomUUID());
                put("edc.iam.sts.oauth.token.url", "http://sts.example.com/token");
                put("edc.iam.sts.oauth.client.id", "test-clientid");
                put("edc.iam.sts.oauth.client.secret.alias", "test-clientid-alias");
                put("tx.edc.iam.sts.dim.url", "http://sts.example.com");
                put("tx.edc.iam.iatp.bdrs.server.url", "http://sts.example.com");
            }
        };

        return ConfigFactory.fromMap(settings);
    }

    public RequestSpecification basePublicApiRequest() {
        return given().baseUri(publicApi.get().toString());
    }

}

