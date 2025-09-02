/*
 * Copyright (c) 2025 Cofinity-X
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

package org.eclipse.tractusx.edc.tests.transfer.extension;

import org.eclipse.edc.connector.controlplane.test.system.utils.LazySupplier;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.GenericContainer;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.util.Map.entry;
import static org.eclipse.edc.util.io.Ports.getFreePort;

public class BdrsServerExtension implements BeforeAllCallback, AfterAllCallback {

    private final LazySupplier<URI> directoryEndpoint = new LazySupplier<>(() -> URI.create("http://localhost:%d%s".formatted(getFreePort(), "/directory")));
    private final LazySupplier<URI> managementEndpoint = new LazySupplier<>(() -> URI.create("http://localhost:%d%s".formatted(getFreePort(), "/management")));

    private final GenericContainer<?> bdrsServer = new GenericContainer<>("tractusx/bdrs-server-memory:0.5.4")
            .withLogConsumer(o -> System.out.println("[BDRS] " + o.getUtf8StringWithoutLineEnding()))
            .withNetworkMode("host");

    private final String issuerDid;

    public BdrsServerExtension(String issuerDid) {
        this.issuerDid = issuerDid;
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        var config = Map.ofEntries(
                entry("edc.iam.issuer.id", UUID.randomUUID().toString()),
                entry("edc.iam.did.web.use.https", "false"),
                entry("edc.iam.trusted-issuer.issuer.id", issuerDid),
                entry("web.http.directory.port", String.valueOf(directoryEndpoint.get().getPort())),
                entry("web.http.directory.path", directoryEndpoint.get().getPath()),
                entry("edc.api.auth.key", "password"),
                entry("web.http.management.port", String.valueOf(managementEndpoint.get().getPort())),
                entry("web.http.management.path", managementEndpoint.get().getPath())
        );
        bdrsServer.withEnv(config).start();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        bdrsServer.stop();
    }

    public Config getConfig() {
        return ConfigFactory.fromMap(Map.of(
            "tx.edc.iam.iatp.bdrs.server.url", directoryEndpoint.get().toString()
        ));
    }

    public final void addMapping(String bpn, String did) {
        given()
                .baseUri(managementEndpoint.get().toString())
                .contentType(JSON)
                .header("x-api-key", "password")
                .body(Map.of("bpn", bpn, "did", did))
                .post("/bpn-directory")
                .then()
                .statusCode(204);
    }
}
