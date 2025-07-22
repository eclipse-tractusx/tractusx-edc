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

import org.assertj.core.api.Assertions;
import org.eclipse.dataspacetck.core.system.ConsoleMonitor;
import org.eclipse.dataspacetck.runtime.TckRuntime;
import org.eclipse.edc.junit.annotations.NightlyTest;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspacetck.core.api.system.SystemsConstants.TCK_LAUNCHER;
import static org.eclipse.edc.util.io.Ports.getFreePort;

@NightlyTest
public class EdcCompatibilityEmbeddedTest {

    private static final URI PROTOCOL_URL = URI.create("http://localhost:" + getFreePort() + "/protocol");
    private static final URI MANAGEMENT_URL = URI.create("http://localhost:" + getFreePort() + "/management");
    private static final URI CONTROL_URL = URI.create("http://localhost:" + getFreePort() + "/control");
    private static final URI VERSION_URL = URI.create("http://localhost:" + getFreePort() + "/version");
    private static final URI WEBHOOK_URL = URI.create("http://localhost:" + getFreePort() + "/tck");
    private static final String DEFAULT_LAUNCHER = "org.eclipse.dataspacetck.dsp.system.DspSystemLauncher";
    private static final String TEST_PACKAGE = "org.eclipse.dataspacetck.dsp.verification";
    public static final String API_KEY = "password";
    protected static final URI DATA_PLANE_PROXY = URI.create("http://localhost:" + getFreePort());
    private static final URI DATA_PLANE_PUBLIC = URI.create("http://localhost:" + getFreePort() + "/public");
    private static final URI FEDERATED_CATALOG = URI.create("http://localhost:" + getFreePort() + "/api/catalog");

    @RegisterExtension
    protected static RuntimeExtension runtime = new RuntimePerClassExtension(new EmbeddedRuntime("CUT",
            ":edc-tests:dsp-compatibility-tests:connector-under-test")
            .registerServiceMock(BdrsClient.class, (s) -> s)
            .configurationProvider(EdcCompatibilityEmbeddedTest::runtimeConfiguration));

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
                put("web.http.protocol.port", String.valueOf(PROTOCOL_URL.getPort()));
                put("web.http.protocol.path", PROTOCOL_URL.getPath());
                put("web.http.tck.port", String.valueOf(WEBHOOK_URL.getPort()));
                put("web.http.tck.path", "/tck");
                put("web.api.auth.key", API_KEY);
                put("edc.dsp.callback.address", PROTOCOL_URL.toString()); // host.docker.internal is required by the container to communicate with the host
                put("edc.management.context.enabled", "true");
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

    private static Map<String, String> loadProperties() throws IOException {
        var properties = new Properties();
        try (var reader = new FileReader(resourceConfig("docker.tck.properties"))) {
            properties.load(reader);
        }

        if (!properties.containsKey(TCK_LAUNCHER)) {
            properties.put(TCK_LAUNCHER, DEFAULT_LAUNCHER);
        }
        properties.put("dataspacetck.dsp.jsonld.context.edc.path", resourceConfig("dspace-edc-context-v1.jsonld"));
        properties.put("dataspacetck.dsp.jsonld.context.edc.uri", "https://w3id.org/edc/dspace/v0.0.1");
        properties.put("dataspacetck.dsp.jsonld.context.tractusx_edc.path", resourceConfig("tx-v1.jsonld"));
        properties.put("dataspacetck.dsp.jsonld.context.tractusx_edc.uri", "https://w3id.org/tractusx/edc/v0.0.1");
        properties.put("dataspacetck.dsp.jsonld.context.tractusx_auth.path", resourceConfig("tx-auth-v1.jsonld"));
        properties.put("dataspacetck.dsp.jsonld.context.tractusx_auth.uri", "https://w3id.org/tractusx/auth/v1.0.0");
        properties.put("dataspacetck.dsp.jsonld.context.tractusx_policy.path", resourceConfig("cx-policy-v1.jsonld"));
        properties.put("dataspacetck.dsp.jsonld.context.tractusx_policy.uri", "https://w3id.org/tractusx/policy/v1.0.0");
        properties.put("dataspacetck.dsp.connector.http.url", PROTOCOL_URL + "/2025-1");
        properties.put("dataspacetck.dsp.connector.http.base.url", PROTOCOL_URL);
        properties.put("dataspacetck.dsp.connector.negotiation.initiate.url", WEBHOOK_URL + "/negotiations/requests");
        properties.put("dataspacetck.dsp.connector.transfer.initiate.url", WEBHOOK_URL + "/transfers/requests");
        return properties.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
    }

    @Timeout(300)
    @Test
    void assertDspCompatibility() throws IOException {

        var monitor = new ConsoleMonitor(true, true);

        var result = TckRuntime.Builder.newInstance()
                .properties(loadProperties())
                .addPackage(TEST_PACKAGE)
                .monitor(monitor)
                .build()
                .execute();

        var failures = result.getFailures().stream()
                .map(this::mapFailure)
                .toList();

        var failureIds = failures.stream()
                .map(TestResult::testId)
                .collect(Collectors.toSet());

        assertThat(failureIds).isEmpty();

        var failureReasons = failures.stream()
                .map(TestResult::format)
                .toList();

        if (!failureReasons.isEmpty()) {
            Assertions.fail(failureReasons.size() + " TCK test cases failed:\n" + String.join("\n", failureReasons));
        }
    }

    private TestResult mapFailure(TestExecutionSummary.Failure failure) {
        var displayName = failure.getTestIdentifier().getDisplayName().split(":");
        return new TestResult(format("%s:%s", displayName[0], displayName[1]), failure);
    }

    private record TestResult(String testId, TestExecutionSummary.Failure failure) {
        public String format() {
            return "- " + failure.getTestIdentifier().getDisplayName() + " (" + failure.getException() + ")";
        }
    }
}

