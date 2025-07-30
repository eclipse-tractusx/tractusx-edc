/********************************************************************************
 * Copyright (c) 2025 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.tests.transfer;

import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.TransferType;
import org.eclipse.tractusx.edc.tests.participant.TransferParticipant;
import org.eclipse.tractusx.edc.tests.runtimes.PostgresExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;

import java.util.UUID;

import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.STARTED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.TERMINATED;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.types.domain.transfer.FlowType.PULL;
import static org.eclipse.edc.spi.types.domain.transfer.FlowType.PUSH;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_NAME;
import static org.eclipse.tractusx.edc.tests.runtimes.Runtimes.pgRuntime;

@EndToEndTest
public class DataFlowApiEndToEndTest {

    private static final TransferParticipant PARTICIPANT = TransferParticipant.Builder.newInstance()
            .name(PROVIDER_NAME)
            .id(PROVIDER_BPN)
            .build();

    @RegisterExtension
    @Order(0)
    private static final PostgresExtension POSTGRES = new PostgresExtension(PARTICIPANT.getName());

    @RegisterExtension
    private static final RuntimeExtension RUNTIME = pgRuntime(PARTICIPANT, POSTGRES);

    private ClientAndServer server;

    @BeforeEach
    void setup() {
        server = ClientAndServer.startClientAndServer("localhost", getFreePort());
    }

    @Test
    void triggerDataTransfer_shouldFail_whenDataFlowDoesNotExist() {
        var dataFlowId = UUID.randomUUID().toString();
        var expectedErrorMessage = "Object of type DataFlow with ID=%s was not found".formatted(dataFlowId);

        var body = triggerDataTransfer(dataFlowId)
                .statusCode(404)
                .extract().body().jsonPath();

        assertThat(body).isNotNull()
                .extracting(this::extractErrorMessage)
                .isEqualTo(expectedErrorMessage);
    }

    @Test
    void triggerDataTransfer_shouldFail_whenDataFlowIsNotPushFlowType() {
        var pullDataFlow = DataFlow.Builder.newInstance()
                .state(STARTED.code())
                .transferType(new TransferType("HttpData", PULL))
                .source(dataAddressBuilder().build())
                .build();
        var expectedErrorMessage = "Could not trigger dataflow %s because it's not PUSH flow type"
                .formatted(pullDataFlow.getId());

        RUNTIME.getService(DataPlaneStore.class).save(pullDataFlow);

        var body = triggerDataTransfer(pullDataFlow.getId())
                .statusCode(400)
                .extract().body().jsonPath();

        assertThat(body).isNotNull()
                .extracting(this::extractErrorMessage)
                .isEqualTo(expectedErrorMessage);
    }

    @Test
    void triggerDataTransfer_shouldFail_whenDataFlowIsFinite() {
        var finiteDataFlow = DataFlow.Builder.newInstance()
                .state(STARTED.code())
                .transferType(new TransferType("HttpData", PUSH))
                .source(dataAddressBuilder().build())
                .build();
        var expectedErrorMessage = "Could not trigger dataflow %s because underlying asset is finite"
                .formatted(finiteDataFlow.getId());

        RUNTIME.getService(DataPlaneStore.class).save(finiteDataFlow);

        var body = triggerDataTransfer(finiteDataFlow.getId())
                .statusCode(400)
                .extract().body().jsonPath();

        assertThat(body).isNotNull()
                .extracting(this::extractErrorMessage)
                .isEqualTo(expectedErrorMessage);
    }

    @Test
    void triggerDataTransfer_shouldFail_whenDataFlowIsNotInStartedState() {
        var terminatedDataFlow = DataFlow.Builder.newInstance()
                .state(TERMINATED.code())
                .transferType(new TransferType("HttpData", PUSH))
                .source(dataAddressBuilder().property(EDC_NAMESPACE + "isNonFinite", "true").build())
                .build();
        var expectedErrorMessage = "Could not trigger dataflow %s because it's not STARTED. Current state is %s"
                .formatted(terminatedDataFlow.getId(), terminatedDataFlow.stateAsString());

        RUNTIME.getService(DataPlaneStore.class).save(terminatedDataFlow);

        var body = triggerDataTransfer(terminatedDataFlow.getId())
                .statusCode(409)
                .extract().body().jsonPath();

        assertThat(body).isNotNull()
                .extracting(this::extractErrorMessage)
                .isEqualTo(expectedErrorMessage);
    }

    @Test
    void trigger_shouldReturnSuccess_whenAllValidationsSucceed() {
        var dataFlow = DataFlow.Builder.newInstance()
                .state(STARTED.code())
                .transferType(new TransferType("destination", PUSH))
                .source(dataAddressBuilder().property(EDC_NAMESPACE + "isNonFinite", "true").build())
                .build();

        RUNTIME.getService(DataPlaneStore.class).save(dataFlow);

        PARTICIPANT.triggerDataTransfer(dataFlow.getId());
    }

    @AfterEach
    void teardown() {
        server.stop();
    }

    @SuppressWarnings("rawtypes")
    private DataAddress.Builder dataAddressBuilder() {
        return DataAddress.Builder.newInstance()
                .property("type", "HttpData")
                .property("name", "dataflow-api-test")
                .property("baseUrl", "https://mock-url.com")
                .property("contentType", "application/json");
    }

    private ValidatableResponse triggerDataTransfer(String dataFlowId) {
        return PARTICIPANT.baseManagementRequest()
                .basePath("/v4alpha/dataflows")
                .when()
                .contentType(JSON)
                .post("/{id}/trigger", dataFlowId)
                .then();
    }

    private String extractErrorMessage(JsonPath body) {
        return body.getString("[0].message");
    }

}
