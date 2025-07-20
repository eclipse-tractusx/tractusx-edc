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

package org.eclipse.tractusx.edc.tests.tck.dsp;

import org.eclipse.edc.junit.annotations.NightlyTest;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.SelinuxContext;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.util.io.Ports.getFreePort;

@NightlyTest
@Testcontainers
public class EdcCompatibilityDockerTest {

    public static final String API_KEY = "password";
    private static final URI PROTOCOL_URL = URI.create("http://host.docker.internal:8282/protocol");
    private static final URI MANAGEMENT_URL = URI.create("http://host.docker.internal:" + getFreePort() + "/management");
    private static final URI CONTROL_URL = URI.create("http://host.docker.internal:" + getFreePort() + "/control");
    private static final URI VERSION_URL = URI.create("http://host.docker.internal:" + getFreePort() + "/version");

    protected static final URI DATA_PLANE_PROXY = URI.create("http://host.docker.internal:" + getFreePort());
    private static final URI DATA_PLANE_PUBLIC = URI.create("http://host.docker.internal:" + getFreePort() + "/public");
    private static final URI FEDERATED_CATALOG = URI.create("http://host.docker.internal:" + getFreePort() + "/api/catalog");

    private static final GenericContainer<?> TCK_CONTAINER = new TckContainer<>("eclipsedataspacetck/dsp-tck-runtime:1.0.0-RC4");
    @RegisterExtension
    protected static RuntimeExtension runtime = new RuntimePerClassExtension(new EmbeddedRuntime("CUT",
            ":edc-tests:dsp-compatibility-tests:connector-under-test")
            .registerServiceMock(BdrsClient.class, (s) -> s)
            .configurationProvider(EdcCompatibilityDockerTest::runtimeConfiguration));


    private static Config runtimeConfiguration() {
        return ConfigFactory.fromMap(new HashMap<>() {
            {
                put("edc.participant.id", "CONNECTOR_UNDER_TEST");
                put("web.http.port", "8080");
                put("web.http.path", "/api");
                put("web.http.version.port", String.valueOf(VERSION_URL.getPort()));
                put("web.http.version.path", VERSION_URL.getPath());
                put("web.http.control.port", String.valueOf(CONTROL_URL.getPort()));
                put("web.http.control.path", CONTROL_URL.getPath());
                put("web.http.management.port", String.valueOf(MANAGEMENT_URL.getPort()));
                put("web.http.management.path", MANAGEMENT_URL.getPath());
                put("web.http.protocol.port", String.valueOf(PROTOCOL_URL.getPort())); // this must match the configured connector url in resources/docker.tck.properties
                put("web.http.protocol.path", PROTOCOL_URL.getPath()); // this must match the configured connector url in resources/docker.tck.properties
                put("web.api.auth.key", "password");
                put("edc.dsp.callback.address", PROTOCOL_URL.toString()); // host.docker.internal is required by the container to communicate with the host
                put("edc.management.context.enabled", "true");
                put("edc.hostname", "host.docker.internal");
                put("edc.component.id", "DSP-compatibility-test");
                put("edc.transfer.proxy.token.signer.privatekey.alias", "private-key");
                put("edc.transfer.proxy.token.verifier.publickey.alias", "public-key");
                put("edc.policy.validation.enabled", "true");
                put("edc.iam.issuer.id", "did:web:CONNECTOR_UNDER_TEST");
                put("edc.iam.sts.oauth.token.url", "http://sts.example.com/token");
                put("edc.iam.sts.oauth.client.id", "test-client-id");
                put("edc.iam.sts.oauth.client.secret.alias", "test-clientid-alias");
                put("tx.edc.iam.iatp.bdrs.server.url", "http://sts.example.com");
                put("web.http.management.auth.key", API_KEY);
                put("tx.edc.dpf.consumer.proxy.port", String.valueOf(DATA_PLANE_PROXY.getPort()));
                put("tx.edc.dpf.consumer.proxy.auth.apikey", API_KEY);
                put("edc.transfer.send.retry.limit", "3");
                put("edc.transfer.send.retry.base-delay.ms", "100");
                put("edc.dataplane.token.expiry", "3");
                put("edc.dataplane.token.expiry.tolerance", "0");
                put("web.http.catalog.port", String.valueOf(FEDERATED_CATALOG.getPort()));
                put("web.http.catalog.path", FEDERATED_CATALOG.getPath());
                put("web.http.catalog.auth.type", "tokenbased");
                put("web.http.catalog.auth.key", API_KEY);
                put("web.http.public.path", DATA_PLANE_PUBLIC.getPath());
                put("web.http.public.port", String.valueOf(DATA_PLANE_PUBLIC.getPort()));
                put("edc.dataplane.api.public.baseurl", "%s/v2/data".formatted(DATA_PLANE_PUBLIC));
                put("edc.catalog.cache.execution.delay.seconds", "2");
                put("edc.catalog.cache.execution.period.seconds", "2");
            }
        });
    }

    private static String resourceConfig(String resource) {
        return Path.of(TestUtils.getResource(resource)).toString();
    }

    @Timeout(300)
    @Test
    void assertDspCompatibility() {

        //pipe the docker container's log to this console at the INFO level
        var monitor = new ConsoleMonitor(">>> TCK Runtime (Docker)", ConsoleMonitor.Level.INFO, true);
        var reporter = new TckTestReporter();

        TCK_CONTAINER.addFileSystemBind(resourceConfig("docker.tck.properties"), "/etc/tck/config.properties", BindMode.READ_ONLY, SelinuxContext.SINGLE);
        TCK_CONTAINER.addFileSystemBind(resourceConfig("dspace-edc-context-v1.jsonld"), "/etc/tck/dspace-edc-context-v1.jsonld", BindMode.READ_ONLY, SelinuxContext.SINGLE);
        TCK_CONTAINER.addFileSystemBind(resourceConfig("tx-v1.jsonld"), "/etc/tck/tx-v1.jsonld", BindMode.READ_ONLY, SelinuxContext.SINGLE);
        TCK_CONTAINER.addFileSystemBind(resourceConfig("tx-auth-v1.jsonld"), "/etc/tck/tx-auth-v1.jsonld", BindMode.READ_ONLY, SelinuxContext.SINGLE);
        TCK_CONTAINER.addFileSystemBind(resourceConfig("cx-policy-v1.jsonld"), "/etc/tck/cx-policy-v1.jsonld", BindMode.READ_ONLY, SelinuxContext.SINGLE);
        TCK_CONTAINER.withExtraHost("host.docker.internal", "host-gateway");
        TCK_CONTAINER.withLogConsumer(outputFrame -> monitor.info(outputFrame.getUtf8String()));
        TCK_CONTAINER.withLogConsumer(reporter);
        TCK_CONTAINER.waitingFor(new LogMessageWaitStrategy().withRegEx(".*Test run complete.*").withStartupTimeout(Duration.ofSeconds(300)));
        TCK_CONTAINER.start();

        var failures = reporter.failures();
        assertThat(failures).isEmpty();
    }
}

