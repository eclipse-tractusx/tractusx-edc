/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.edc.tests.transfer;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.tractusx.edc.tests.kafka.KafkaExtension;
import org.eclipse.tractusx.edc.tests.participant.TransferParticipant;
import org.eclipse.tractusx.edc.tests.runtimes.PostgresExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_DID;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_NAME;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.DSP_2025;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.DSP_2025_PATH;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_DID;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_NAME;
import static org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions.bpnPolicy;
import static org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase.ASYNC_TIMEOUT;
import static org.eclipse.tractusx.edc.tests.runtimes.Runtimes.pgRuntime;

/**
 * End-to-end test for the {@code KafkaBroker-PULL} transfer type. Runs a real Kafka broker via
 * Testcontainers and a WireMock-backed OAuth2 token endpoint.
 */
@EndToEndTest
public class KafkaPullEndToEndTest {

    private static final String TOPIC = "test-topic";
    private static final String CLIENT_ID = "kafka-client-id";
    private static final String CLIENT_SECRET_KEY = "kafka-client-secret";
    private static final String CLIENT_SECRET_VALUE = "kafka-client-secret-value";
    private static final String ACCESS_TOKEN = "test-access-token";

    private static final TransferParticipant CONSUMER = TransferParticipant.Builder.newInstance()
            .name(CONSUMER_NAME)
            .id(CONSUMER_DID)
            .bpn(CONSUMER_BPN)
            .protocol(DSP_2025, DSP_2025_PATH)
            .build();

    private static final TransferParticipant PROVIDER = TransferParticipant.Builder.newInstance()
            .name(PROVIDER_NAME)
            .id(PROVIDER_DID)
            .bpn(PROVIDER_BPN)
            .protocol(DSP_2025, DSP_2025_PATH)
            .build();

    @RegisterExtension
    @Order(0)
    private static final PostgresExtension POSTGRES = new PostgresExtension(PROVIDER.getName(), CONSUMER.getName());

    @RegisterExtension
    private static final RuntimeExtension PROVIDER_RUNTIME = pgRuntime(PROVIDER, POSTGRES, PROVIDER::getConfig);

    @RegisterExtension
    private static final RuntimeExtension CONSUMER_RUNTIME = pgRuntime(CONSUMER, POSTGRES, CONSUMER::getConfig);

    @RegisterExtension
    private static final KafkaExtension KAFKA = new KafkaExtension();

    @RegisterExtension
    private static final WireMockExtension OAUTH = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @BeforeAll
    static void beforeAll() {
        CONSUMER.setJsonLd(CONSUMER_RUNTIME.getService(JsonLd.class));
    }

    @BeforeEach
    void beforeEach() {
        // the provider's Kafka data plane mints (and revokes) the consumer token against this OAuth2 endpoint
        OAUTH.stubFor(post(urlEqualTo("/token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\":\"" + ACCESS_TOKEN + "\",\"expires_in\":3600}")));
        OAUTH.stubFor(post(urlEqualTo("/revoke"))
                .willReturn(aResponse().withStatus(200)));
        PROVIDER_RUNTIME.getService(Vault.class).storeSecret(CLIENT_SECRET_KEY, CLIENT_SECRET_VALUE);
    }

    @Test
    void kafkaPullTransfer_consumerReceivesMessages() {
        KAFKA.createTopic(TOPIC);
        KAFKA.produce(TOPIC, "k1", "hello");
        KAFKA.produce(TOPIC, "k2", "world");

        var transferProcessId = startKafkaPullTransfer("kafka-test-asset", "def-1", kafkaSourceAddress("kafka-transfer-test", true));
        CONSUMER.waitForTransferProcess(transferProcessId, STARTED);

        // the consumer receives an EDR carrying the broker connection details and the minted token
        var edr = CONSUMER.edrs().waitForEdr(transferProcessId);
        assertThat(edr.getString("kafka.bootstrap.servers")).isEqualTo(KAFKA.getBootstrapServers());
        assertThat(edr.getString("topic")).isEqualTo(TOPIC);
        assertThat(edr.getString("kafka.security.protocol")).isEqualTo("SASL_PLAINTEXT");
        assertThat(edr.getString("kafka.sasl.mechanism")).isEqualTo("OAUTHBEARER");
        assertThat(edr.getString("kafka.group.prefix")).isEqualTo(CONSUMER.getBpn());
        assertThat(edr.getString("token")).isEqualTo(ACCESS_TOKEN);

        // consuming via the broker coordinates and topic taken from the EDR returns the published messages
        var consumed = KAFKA.consume(edr.getString("kafka.bootstrap.servers"), edr.getString("topic"), Duration.ofSeconds(10));
        assertThat(consumed).isNotEmpty();

        // the data plane minted the access token via the OAuth2 client-credentials flow
        OAUTH.verify(postRequestedFor(urlEqualTo("/token")));
    }

    @Test
    void kafkaPullTransfer_withoutGroupPrefix_provisionsViaParticipantFallback() {
        // the asset omits kafka.group.prefix, so the data plane must fall back to the consumer participant id
        var transferProcessId = startKafkaPullTransfer("kafka-fallback-asset", "def-2", kafkaSourceAddress("kafka-fallback-test", false));
        CONSUMER.waitForTransferProcess(transferProcessId, STARTED);

        // provisioning still succeeds and the EDR carries a (non-blank) group prefix from the participant-id fallback
        var edr = CONSUMER.edrs().waitForEdr(transferProcessId);
        assertThat(edr.getString("kafka.group.prefix")).isNotBlank();
        OAUTH.verify(postRequestedFor(urlEqualTo("/token")));
    }

    @Test
    void kafkaPullTransfer_terminationRevokesToken() {
        var transferProcessId = startKafkaPullTransfer("kafka-terminate-asset", "def-3", kafkaSourceAddress("kafka-terminate-test", true));
        CONSUMER.waitForTransferProcess(transferProcessId, STARTED);

        // a token was minted (and stored under the flow id) while the transfer was active
        OAUTH.verify(postRequestedFor(urlEqualTo("/token")));

        // terminating the transfer deprovisions the Kafka flow, which revokes the OAuth2 token
        CONSUMER.terminateTransfer(transferProcessId);
        CONSUMER.waitForTransferProcess(transferProcessId, TERMINATED);

        await().atMost(ASYNC_TIMEOUT).untilAsserted(() ->
                OAUTH.verify(postRequestedFor(urlEqualTo("/revoke"))));
    }

    /**
     * Registers the Kafka asset/policy/contract on the provider and starts a {@code KafkaBroker-PULL}
     * transfer from the consumer, returning the transfer-process id.
     */
    private String startKafkaPullTransfer(String assetId, String contractDefinitionId, Map<String, Object> dataAddress) {
        PROVIDER.createAsset(assetId, Map.of(), dataAddress);
        var policyId = PROVIDER.createPolicyDefinition(bpnPolicy(CONSUMER.getBpn()));
        PROVIDER.createContractDefinition(assetId, contractDefinitionId, policyId, policyId);

        return CONSUMER.requestAssetFrom(assetId, PROVIDER)
                .withTransferType("KafkaBroker-PULL")
                .withDestination(kafkaPullDestination())
                .execute();
    }

    /**
     * Builds a {@code KafkaBroker} source {@link Map} data address. When {@code withGroupPrefix} is
     * {@code false} the {@code kafka.group.prefix} property is omitted to exercise the
     * participant-id fallback.
     */
    private Map<String, Object> kafkaSourceAddress(String name, boolean withGroupPrefix) {
        var dataAddress = new HashMap<String, Object>();
        dataAddress.put("name", name);
        dataAddress.put(TYPE, "DataAddress");
        dataAddress.put("type", "KafkaBroker");
        dataAddress.put("topic", TOPIC);
        dataAddress.put("kafka.bootstrap.servers", KAFKA.getBootstrapServers());
        dataAddress.put("kafka.security.protocol", "SASL_PLAINTEXT");
        dataAddress.put("kafka.sasl.mechanism", "OAUTHBEARER");
        dataAddress.put("kafka.poll.duration", "PT1S");
        dataAddress.put("tokenUrl", OAUTH.baseUrl() + "/token");
        dataAddress.put("revokeUrl", OAUTH.baseUrl() + "/revoke");
        dataAddress.put("clientId", CLIENT_ID);
        dataAddress.put("clientSecretKey", CLIENT_SECRET_KEY);
        if (withGroupPrefix) {
            dataAddress.put("kafka.group.prefix", CONSUMER.getBpn());
        }
        return dataAddress;
    }

    private JsonObject kafkaPullDestination() {
        return Json.createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "DataAddress")
                .add(EDC_NAMESPACE + "type", "HttpData")
                .add(EDC_NAMESPACE + "baseUrl", "http://placeholder")
                .build();
    }
}
