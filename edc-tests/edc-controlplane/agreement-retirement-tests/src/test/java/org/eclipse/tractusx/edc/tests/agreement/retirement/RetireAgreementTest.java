package org.eclipse.tractusx.edc.tests.agreement.retirement;

import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import net.minidev.json.JSONObject;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions;
import org.eclipse.tractusx.edc.tests.participant.TransferParticipant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_NAME;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_NAME;
import static org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase.ASYNC_POLL_INTERVAL;
import static org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase.ASYNC_TIMEOUT;
import static org.eclipse.tractusx.edc.tests.runtimes.Runtimes.memoryRuntime;
import static org.eclipse.tractusx.edc.tests.runtimes.Runtimes.pgRuntime;
import static org.mockserver.model.HttpRequest.request;

public class RetireAgreementTest {

    protected static final TransferParticipant CONSUMER = TransferParticipant.Builder.newInstance()
            .name(CONSUMER_NAME)
            .id(CONSUMER_BPN)
            .build();

    protected static final TransferParticipant PROVIDER = TransferParticipant.Builder.newInstance()
            .name(PROVIDER_NAME)
            .id(PROVIDER_BPN)
            .build();


    abstract static class Tests {

        ClientAndServer server;

        @BeforeEach
        void setup() {
            server = ClientAndServer.startClientAndServer("localhost", getFreePort());
        }

        @Test
        @DisplayName("Verify all existing TPs related to an agreement are terminated upon its retirement")
        void retireAgreement_shouldCloseTransferProcesses() throws IOException {

            var assetId = "api-asset-1";

            Map<String, Object> dataAddress = Map.of(
                    "name", "transfer-test",
                    "baseUrl", "https://mock-url.com",
                    "type", "HttpData",
                    "contentType", "application/json"
            );

            PROVIDER.createAsset(assetId, Map.of(), dataAddress);

            PROVIDER.storeBusinessPartner(CONSUMER.getBpn(), "test-group1");
            var accessPolicy = PROVIDER.createPolicyDefinition(PolicyHelperFunctions.bpnGroupPolicy(Operator.EQ, "test-group1"));
            var policy = PolicyHelperFunctions.frameworkPolicy(Map.of());
            var contractPolicy = PROVIDER.createPolicyDefinition(policy);
            PROVIDER.createContractDefinition(assetId, "def-1", accessPolicy, contractPolicy);

            var contractAgreementId = CONSUMER.negotiateContract(PROVIDER, policy);

            var privateProperties = Json.createObjectBuilder().build();
            var dataDestination = Json.createObjectBuilder().add("type", "HttpData").build();

            var transferProcessId = CONSUMER.initiateTransfer(PROVIDER, contractAgreementId, privateProperties, dataDestination, "HttpData-PULL");

            await().pollInterval(ASYNC_POLL_INTERVAL)
                    .atMost(ASYNC_TIMEOUT)
                    .untilAsserted(() -> {
                        String state = CONSUMER.getTransferProcessState(transferProcessId);
                        Assertions.assertThat(state).isEqualTo(ContractNegotiationStates.FINALIZED.name());
                    });

        }

        @AfterEach
        void teardown() throws IOException {
            server.stop();
        }
    }

    @Nested
    @EndToEndTest
    class InMemory extends Tests {

        @RegisterExtension
        protected static final RuntimeExtension CONSUMER_RUNTIME = memoryRuntime(CONSUMER.getName(), CONSUMER.getBpn(), CONSUMER.getConfiguration());

        @RegisterExtension
        protected static final RuntimeExtension PROVIDER_RUNTIME = memoryRuntime(PROVIDER.getName(), PROVIDER.getBpn(), PROVIDER.getConfiguration());

    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {

        @RegisterExtension
        protected static final RuntimeExtension CONSUMER_RUNTIME = pgRuntime(CONSUMER.getName(), CONSUMER.getBpn(), CONSUMER.getConfiguration());

        @RegisterExtension
        protected static final RuntimeExtension PROVIDER_RUNTIME = pgRuntime(PROVIDER.getName(), PROVIDER.getBpn(), PROVIDER.getConfiguration());

    }
}
