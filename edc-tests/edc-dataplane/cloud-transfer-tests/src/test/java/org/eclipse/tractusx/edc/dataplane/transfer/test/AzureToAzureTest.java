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

package org.eclipse.tractusx.edc.dataplane.transfer.test;

import io.restassured.http.ContentType;
import org.eclipse.edc.azure.testfixtures.annotations.AzureStorageIntegrationTest;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.AZBLOB_CONSUMER_ACCOUNT_KEY;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.AZBLOB_CONSUMER_ACCOUNT_NAME;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.AZBLOB_CONSUMER_CONTAINER_NAME;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.AZBLOB_CONSUMER_KEY_ALIAS;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.AZBLOB_PROVIDER_ACCOUNT_KEY;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.AZBLOB_PROVIDER_ACCOUNT_NAME;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.AZBLOB_PROVIDER_CONTAINER_NAME;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.AZBLOB_PROVIDER_KEY_ALIAS;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.AZURITE_CONTAINER_PORT;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.AZURITE_DOCKER_IMAGE;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.TESTFILE_NAME;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.blobDestinationAddress;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.blobSourceAddress;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestFunctions.createSparseFile;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

/**
 * This test is intended to verify transfers within the same cloud provider, i.e. AzureBlob-to-AzureBlob.
 * It spins up a fully-fledged dataplane and issues the DataFlowRequest via the data plane's Control API
 */
@SuppressWarnings("resource")
@Testcontainers
@AzureStorageIntegrationTest
public class AzureToAzureTest {
    private static final int PROVIDER_CONTROL_PORT = getFreePort();

    private static final int AZURITE_HOST_PORT = getFreePort();
    // launches the data plane
    @RegisterExtension
    protected static final ParticipantRuntime DATAPLANE_RUNTIME = new ParticipantRuntime(
            ":edc-tests:runtime:dataplane-cloud",
            "AzureBlob-Dataplane",
            RuntimeConfig.Azure.createDataplane("/control", PROVIDER_CONTROL_PORT, AZURITE_HOST_PORT)
    );
    /**
     * Currently we have to use one container to host both consumer and provider accounts, because we cannot handle
     * two different endpoint templates for provider and consumer. Endpoint templates are configured globally.
     * Also, the host-port must be fixed/deterministic, as the {@code PROVIDER_RUNTIME} needs to know it in advance
     */
    @Container
    private final FixedHostPortGenericContainer<?> azuriteContainer = new FixedHostPortGenericContainer<>(AZURITE_DOCKER_IMAGE)
            .withFixedExposedPort(AZURITE_HOST_PORT, AZURITE_CONTAINER_PORT)
            .withEnv("AZURITE_ACCOUNTS", AZBLOB_PROVIDER_ACCOUNT_NAME + ":" + AZBLOB_PROVIDER_ACCOUNT_KEY + ";" + AZBLOB_CONSUMER_ACCOUNT_NAME + ":" + AZBLOB_CONSUMER_ACCOUNT_KEY);
    private AzureBlobHelper providerBlobHelper;
    private AzureBlobHelper consumerBlobHelper;

    @BeforeEach
    void setup() {
        providerBlobHelper = new AzureBlobHelper(AZBLOB_PROVIDER_ACCOUNT_NAME, AZBLOB_PROVIDER_ACCOUNT_KEY, azuriteContainer.getHost(), azuriteContainer.getMappedPort(AZURITE_CONTAINER_PORT));
        consumerBlobHelper = new AzureBlobHelper(AZBLOB_CONSUMER_ACCOUNT_NAME, AZBLOB_CONSUMER_ACCOUNT_KEY, azuriteContainer.getHost(), azuriteContainer.getMappedPort(AZURITE_CONTAINER_PORT));
    }

    @Test
    void transferFile_success() {
        // upload file to provider's blob store
        var bcc = providerBlobHelper.createContainer(AZBLOB_PROVIDER_CONTAINER_NAME);
        providerBlobHelper.uploadBlob(bcc, TESTFILE_NAME);

        // create container in consumer's blob store
        consumerBlobHelper.createContainer(AZBLOB_CONSUMER_CONTAINER_NAME);

        DATAPLANE_RUNTIME.getVault().storeSecret(AZBLOB_PROVIDER_KEY_ALIAS, AZBLOB_PROVIDER_ACCOUNT_KEY);
        DATAPLANE_RUNTIME.getVault().storeSecret(AZBLOB_CONSUMER_KEY_ALIAS, """
                {"sas": "%s","edctype":"dataspaceconnector:azuretoken"}
                """.formatted(consumerBlobHelper.generateAccountSas(AZBLOB_CONSUMER_CONTAINER_NAME)));

        var request = createFlowRequest(TESTFILE_NAME);

        var url = "http://localhost:%s/control/transfer".formatted(PROVIDER_CONTROL_PORT);

        given().when()
                .baseUri(url)
                .contentType(ContentType.JSON)
                .body(request)
                .post()
                .then()
                .log().ifValidationFails()
                .log().ifError()
                .statusCode(200);

        await().pollInterval(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(60))
                .untilAsserted(() -> assertThat(consumerBlobHelper.listBlobs(AZBLOB_CONSUMER_CONTAINER_NAME))
                        .isNotEmpty()
                        .contains(TESTFILE_NAME));

    }

    /**
     * This method is used to transfer a large sparse file from the provider's blob store to the consumer's blob store. It follows the following steps:
     * <ol>
     * <li>Uploads the file to the provider's blob store.</li>
     * <li>Creates a random binary file of size greater than 1GB.</li>
     * <li>Uploads the large file to the provider's blob store.</li>
     * <li>Creates a container in the consumer's blob store.</li>
     * <li>Creates a flow request with the given blob name.</li>
     * <li>Waits for the transfer to complete by polling the consumer's blob store for the existence of the transferred blob.</li>
     * </ol>
     */
    @ParameterizedTest(name = "File size bytes: {0}")
    // 1mb, 512mb, 1gb
    @ValueSource(longs = {1024 * 1024 * 512, 1024L * 1024L * 1024L, /*1024L * 1024L * 1024L * 1024 takes extremely long!*/})
    void transferFile_largeFile(long sizeBytes) throws IOException {
        // upload file to provider's blob store
        var bcc = providerBlobHelper.createContainer(AZBLOB_PROVIDER_CONTAINER_NAME);

        // create random binary file of >1gb in size
        var blobName = "largeblob.bin";
        var f = createSparseFile(sizeBytes);
        providerBlobHelper.uploadBlob(bcc, new FileInputStream(f), blobName);

        // create container in consumer's blob store
        consumerBlobHelper.createContainer(AZBLOB_CONSUMER_CONTAINER_NAME);

        DATAPLANE_RUNTIME.getVault().storeSecret(AZBLOB_PROVIDER_KEY_ALIAS, AZBLOB_PROVIDER_ACCOUNT_KEY);
        DATAPLANE_RUNTIME.getVault().storeSecret(AZBLOB_CONSUMER_KEY_ALIAS, """
                {"sas": "%s","edctype":"dataspaceconnector:azuretoken"}
                """.formatted(consumerBlobHelper.generateAccountSas(AZBLOB_CONSUMER_CONTAINER_NAME)));

        var request = createFlowRequest(blobName);

        var url = "http://localhost:%s/control/transfer".formatted(PROVIDER_CONTROL_PORT);

        given().when()
                .baseUri(url)
                .contentType(ContentType.JSON)
                .body(request)
                .post()
                .then()
                .log().ifValidationFails()
                .log().ifError()
                .statusCode(200);

        await().pollInterval(Duration.ofSeconds(10))
                .atMost(Duration.ofSeconds(120))
                .untilAsserted(() -> assertThat(consumerBlobHelper.listBlobs(AZBLOB_CONSUMER_CONTAINER_NAME))
                        .isNotEmpty()
                        .contains(blobName));

    }

    @Test
    void transferFile_targetContainerNotExist_shouldFail() {
        // upload file to provider's blob store
        var bcc = providerBlobHelper.createContainer(AZBLOB_PROVIDER_CONTAINER_NAME);
        providerBlobHelper.uploadBlob(bcc, TESTFILE_NAME);

        // do NOT create container in consumer's blob store

        DATAPLANE_RUNTIME.getVault().storeSecret(AZBLOB_PROVIDER_KEY_ALIAS, AZBLOB_PROVIDER_ACCOUNT_KEY);
        DATAPLANE_RUNTIME.getVault().storeSecret(AZBLOB_CONSUMER_KEY_ALIAS, """
                {"sas": "%s","edctype":"dataspaceconnector:azuretoken"}
                """.formatted(consumerBlobHelper.generateAccountSas(AZBLOB_CONSUMER_CONTAINER_NAME)));

        var request = createFlowRequest(TESTFILE_NAME);

        var url = "http://localhost:%s/control/transfer".formatted(PROVIDER_CONTROL_PORT);

        given().when()
                .baseUri(url)
                .contentType(ContentType.JSON)
                .body(request)
                .post()
                .then()
                .log().ifValidationFails()
                .log().ifError()
                .statusCode(200);

        // wait until the data plane logs an exception that it cannot transfer the blob
        await().pollInterval(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> verify(DATAPLANE_RUNTIME.getContext().getMonitor())
                        .severe(contains("Error creating blob %s on account %s".formatted(TESTFILE_NAME, AZBLOB_CONSUMER_ACCOUNT_NAME)), isA(IOException.class)));
    }

    private DataFlowRequest createFlowRequest(String blobName) {
        return DataFlowRequest.Builder.newInstance()
                .id("test-request")
                .sourceDataAddress(blobSourceAddress(blobName))
                .destinationDataAddress(blobDestinationAddress(blobName))
                .processId("test-process-id")
                .trackable(false)
                .build();
    }

}
