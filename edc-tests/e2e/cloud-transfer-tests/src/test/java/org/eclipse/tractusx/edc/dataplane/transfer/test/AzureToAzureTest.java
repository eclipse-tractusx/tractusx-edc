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

import com.azure.core.util.BinaryData;
import io.restassured.http.ContentType;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.junit.utils.LazySupplier;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.tractusx.edc.tests.azure.AzureBlobClient;
import org.eclipse.tractusx.edc.tests.azure.AzuriteExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.IntStream;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage.EDC_DATA_FLOW_START_MESSAGE_TYPE;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.AZBLOB_CONSUMER_KEY_ALIAS;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.AZBLOB_PROVIDER_KEY_ALIAS;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.CONSUMER_AZURITE_ACCOUNT;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.PREFIX_FOR_MUTIPLE_FILES;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.PROVIDER_AZURITE_ACCOUNT;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.TESTFILE_NAME;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestFunctions.createSparseFile;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

/**
 * This test is intended to verify transfers within the same cloud provider, i.e. AzureBlob-to-AzureBlob.
 * It spins up a fully-fledged dataplane and issues the DataFlowStartMessage via the data plane's Control API
 */
@SuppressWarnings("resource")
@Testcontainers
@EndToEndTest
public class AzureToAzureTest {

    private static final LazySupplier<URI> CONTROL_API_URI = new LazySupplier<>(() -> URI.create("http://localhost:%s/control".formatted(getFreePort())));
    private static final LazySupplier<URI> START_DATAFLOW_URI = new LazySupplier<>(() -> URI.create("%s/v1/dataflows".formatted(CONTROL_API_URI.get())));
    private static final int AZURITE_HOST_PORT = getFreePort();

    @RegisterExtension
    protected static final RuntimeExtension DATAPLANE_RUNTIME = new RuntimePerClassExtension(new EmbeddedRuntime(
            "AzureBlob-Dataplane",
            ":edc-tests:runtime:dataplane-cloud"
    ).configurationProvider(() -> RuntimeConfig.Azure.blobstoreDataplaneConfig(CONTROL_API_URI, AZURITE_HOST_PORT))).registerServiceMock(Monitor.class, spy(new ConsoleMonitor("AzureBlob-Dataplane", ConsoleMonitor.Level.DEBUG, true)));

    /**
     * Currently we have to use one container to host both consumer and provider accounts, because we cannot handle
     * two different endpoint templates for provider and consumer. Endpoint templates are configured globally.
     * Also, the host-port must be fixed/deterministic, as the {@code PROVIDER_RUNTIME} needs to know it in advance
     */
    @RegisterExtension
    private static final AzuriteExtension AZURITE_CONTAINER = new AzuriteExtension(AZURITE_HOST_PORT, PROVIDER_AZURITE_ACCOUNT, CONSUMER_AZURITE_ACCOUNT);

    private AzureBlobClient providerBlobHelper;
    private AzureBlobClient consumerBlobHelper;

    @BeforeEach
    void setup() {
        providerBlobHelper = AZURITE_CONTAINER.getClientFor(PROVIDER_AZURITE_ACCOUNT);
        consumerBlobHelper = AZURITE_CONTAINER.getClientFor(CONSUMER_AZURITE_ACCOUNT);
    }

    @Test
    void transferMultipleFile_success(Vault vault) {
        var sourceContainerName = UUID.randomUUID().toString();
        var sourceContainer = providerBlobHelper.createContainer(sourceContainerName);
        var filesNames = new ArrayDeque<String>();

        var fileData = BinaryData.fromString(TestUtils.getResourceFileContentAsString(TESTFILE_NAME));
        var fileNames = IntStream.rangeClosed(1, 2).mapToObj(i -> PREFIX_FOR_MUTIPLE_FILES + i + '_' + TESTFILE_NAME).toList();
        fileNames.forEach(filename -> providerBlobHelper.uploadBlob(sourceContainer, fileData, filename));

        var destinationContainerName = UUID.randomUUID().toString();
        consumerBlobHelper.createContainer(destinationContainerName);
        vault.storeSecret(AZBLOB_PROVIDER_KEY_ALIAS, PROVIDER_AZURITE_ACCOUNT.key());
        vault.storeSecret(AZBLOB_CONSUMER_KEY_ALIAS, """
                {"sas": "%s","edctype":"dataspaceconnector:azuretoken"}
                """.formatted(consumerBlobHelper.generateAccountSas(destinationContainerName)));

        var request = createFlowRequestBuilder(
                blobAddress(sourceContainerName, PROVIDER_AZURITE_ACCOUNT.name(), AZBLOB_PROVIDER_KEY_ALIAS, dspaceProperty(EDC_NAMESPACE + "blobPrefix", PREFIX_FOR_MUTIPLE_FILES)),
                blobAddress(destinationContainerName, CONSUMER_AZURITE_ACCOUNT.name(), AZBLOB_CONSUMER_KEY_ALIAS, dspaceProperty(EDC_NAMESPACE + "blobName", "any"))
        ).build();

        given().when()
                .baseUri(START_DATAFLOW_URI.get().toString())
                .contentType(ContentType.JSON)
                .body(request)
                .post()
                .then()
                .log().ifValidationFails()
                .statusCode(200);

        await().pollInterval(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(60))
                .untilAsserted(() -> assertThat(consumerBlobHelper.listBlobs(destinationContainerName))
                        .isNotEmpty()
                        .containsAll(filesNames));
    }

    @Test
    void transferFile_success(Vault vault) {
        // upload file to provider's blob store
        var sourceContainerName = UUID.randomUUID().toString();
        var sourceContainer = providerBlobHelper.createContainer(sourceContainerName);
        var fileData = BinaryData.fromString(TestUtils.getResourceFileContentAsString(TESTFILE_NAME));

        providerBlobHelper.uploadBlob(sourceContainer, fileData, TESTFILE_NAME);

        // create container in consumer's blob store
        var destinationContainerName = UUID.randomUUID().toString();
        consumerBlobHelper.createContainer(destinationContainerName);

        vault.storeSecret(AZBLOB_PROVIDER_KEY_ALIAS, PROVIDER_AZURITE_ACCOUNT.key());
        vault.storeSecret(AZBLOB_CONSUMER_KEY_ALIAS, """
                {"sas": "%s","edctype":"dataspaceconnector:azuretoken"}
                """.formatted(consumerBlobHelper.generateAccountSas(destinationContainerName)));

        given().when()
                .baseUri(START_DATAFLOW_URI.get().toString())
                .contentType(ContentType.JSON)
                .body(createFlowRequestBuilder(
                        blobAddress(sourceContainerName, PROVIDER_AZURITE_ACCOUNT.name(), AZBLOB_PROVIDER_KEY_ALIAS, dspaceProperty(EDC_NAMESPACE + "blobName", TESTFILE_NAME)),
                        blobAddress(destinationContainerName, CONSUMER_AZURITE_ACCOUNT.name(), AZBLOB_CONSUMER_KEY_ALIAS, dspaceProperty(EDC_NAMESPACE + "blobName", TESTFILE_NAME))
                ).build())
                .post()
                .then()
                .log().ifValidationFails()
                .statusCode(200);

        await().pollInterval(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(60))
                .untilAsserted(() -> assertThat(consumerBlobHelper.listBlobs(destinationContainerName))
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
    @ValueSource(longs = { 1024 * 1024 * 512, 1024L * 1024L * 1024L, /*1024L * 1024L * 1024L * 1024 takes extremely long!*/ })
    void transferFile_largeFile(long sizeBytes, Vault vault) throws IOException {
        // upload file to provider's blob store
        var sourceContainerName = UUID.randomUUID().toString();
        var bcc = providerBlobHelper.createContainer(sourceContainerName);

        // create random binary file of >1gb in size
        var blobName = "largeblob.bin";
        var f = createSparseFile(sizeBytes);
        providerBlobHelper.uploadBlob(bcc, new FileInputStream(f), blobName);

        // create container in consumer's blob store
        var destinationContainerName = UUID.randomUUID().toString();
        consumerBlobHelper.createContainer(destinationContainerName);

        vault.storeSecret(AZBLOB_PROVIDER_KEY_ALIAS, PROVIDER_AZURITE_ACCOUNT.key());
        vault.storeSecret(AZBLOB_CONSUMER_KEY_ALIAS, """
                {"sas": "%s","edctype":"dataspaceconnector:azuretoken"}
                """.formatted(consumerBlobHelper.generateAccountSas(destinationContainerName)));

        given().when()
                .baseUri(START_DATAFLOW_URI.get().toString())
                .contentType(ContentType.JSON)
                .body(createFlowRequestBuilder(
                        blobAddress(sourceContainerName, PROVIDER_AZURITE_ACCOUNT.name(), AZBLOB_PROVIDER_KEY_ALIAS, dspaceProperty(EDC_NAMESPACE + "blobName", blobName)),
                        blobAddress(destinationContainerName, CONSUMER_AZURITE_ACCOUNT.name(), AZBLOB_CONSUMER_KEY_ALIAS, dspaceProperty(EDC_NAMESPACE + "blobName", blobName))
                ).build())
                .post()
                .then()
                .log().ifValidationFails()
                .log().ifValidationFails()
                .statusCode(200);

        await().pollInterval(Duration.ofSeconds(10))
                .atMost(Duration.ofSeconds(120))
                .untilAsserted(() -> assertThat(consumerBlobHelper.listBlobs(destinationContainerName))
                        .isNotEmpty()
                        .contains(blobName));

    }

    @Test
    void transferFolder_targetFolderNotExists_shouldCreate(Vault vault) {

        vault.storeSecret(AZBLOB_PROVIDER_KEY_ALIAS, PROVIDER_AZURITE_ACCOUNT.key());
        var destinationContainerName = UUID.randomUUID().toString();
        var sas = consumerBlobHelper.generateAccountSas(destinationContainerName);
        vault.storeSecret(AZBLOB_CONSUMER_KEY_ALIAS, """
                {"sas": "%s","edctype":"dataspaceconnector:azuretoken"}
                """.formatted(sas));

        // create container in consumer's blob store
        consumerBlobHelper.createContainer(destinationContainerName);

        var sourceContainerName = UUID.randomUUID().toString();
        var sourceContainer = providerBlobHelper.createContainer(sourceContainerName);
        var fileData = BinaryData.fromString(TestUtils.getResourceFileContentAsString(TESTFILE_NAME));

        providerBlobHelper.uploadBlob(sourceContainer, fileData, "folder/blob.bin");
        providerBlobHelper.uploadBlob(sourceContainer, fileData, "folder/blob2.bin");
        providerBlobHelper.uploadBlob(sourceContainer, fileData, "folder/blob3.bin");

        var request = createFlowRequestBuilder(
                blobAddress(sourceContainerName, PROVIDER_AZURITE_ACCOUNT.name(), AZBLOB_PROVIDER_KEY_ALIAS, dspaceProperty(EDC_NAMESPACE + "blobPrefix", "folder/")),
                blobAddress(destinationContainerName, CONSUMER_AZURITE_ACCOUNT.name(), AZBLOB_CONSUMER_KEY_ALIAS, dspaceProperty(EDC_NAMESPACE + "folderName", "destfolder"))
        ).build();

        given().when()
                .baseUri(START_DATAFLOW_URI.get().toString())
                .contentType(ContentType.JSON)
                .body(request)
                .post()
                .then()
                .log().ifValidationFails()
                .statusCode(200);

        await().pollInterval(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(60))
                .untilAsserted(() -> assertThat(consumerBlobHelper.listBlobs(destinationContainerName))
                        .isNotEmpty()
                        .contains("destfolder/folder/blob.bin", "destfolder/folder/blob2.bin", "destfolder/folder/blob3.bin"));
    }

    @Test
    void transferFile_targetContainerNotExist_shouldFail(Vault vault) {
        var sourceContainerName = UUID.randomUUID().toString();
        var sourceContainer = providerBlobHelper.createContainer(sourceContainerName);
        var fileData = BinaryData.fromString(TestUtils.getResourceFileContentAsString(TESTFILE_NAME));

        providerBlobHelper.uploadBlob(sourceContainer, fileData, TESTFILE_NAME);

        vault.storeSecret(AZBLOB_PROVIDER_KEY_ALIAS, PROVIDER_AZURITE_ACCOUNT.key());
        var destinationContainerName = UUID.randomUUID().toString();
        vault.storeSecret(AZBLOB_CONSUMER_KEY_ALIAS, """
                {"sas": "%s","edctype":"dataspaceconnector:azuretoken"}
                """.formatted(consumerBlobHelper.generateAccountSas(destinationContainerName)));

        given().when()
                .baseUri(START_DATAFLOW_URI.get().toString())
                .contentType(ContentType.JSON)
                .body(
                    createFlowRequestBuilder(
                        blobAddress(sourceContainerName, PROVIDER_AZURITE_ACCOUNT.name(), AZBLOB_PROVIDER_KEY_ALIAS, dspaceProperty(EDC_NAMESPACE + "blobName", TESTFILE_NAME)),
                        blobAddress(destinationContainerName, CONSUMER_AZURITE_ACCOUNT.name(), AZBLOB_CONSUMER_KEY_ALIAS, dspaceProperty(EDC_NAMESPACE + "blobName", TESTFILE_NAME))
                    ).build()
                )
                .post()
                .then()
                .log().ifValidationFails()
                .statusCode(200);

        await().pollInterval(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> verify(DATAPLANE_RUNTIME.getService(Monitor.class))
                        .severe(contains("Error creating blob %s on account %s".formatted(TESTFILE_NAME, CONSUMER_AZURITE_ACCOUNT.name())), isA(IOException.class)));
    }

    private JsonObjectBuilder createFlowRequestBuilder(JsonObjectBuilder sourceDataAddress, JsonObjectBuilder destinationDataAddress) {
        return Json.createObjectBuilder()
                .add("@context", Json.createObjectBuilder().add("@vocab", EDC_NAMESPACE).add("dspace", "https://w3id.org/dspace/v0.8/"))
                .add("@type", EDC_DATA_FLOW_START_MESSAGE_TYPE)
                .add("@id", UUID.randomUUID().toString())
                .add("processId", UUID.randomUUID().toString())
                .add("sourceDataAddress", sourceDataAddress)
                .add("destinationDataAddress", destinationDataAddress)
                .add("flowType", "PUSH")
                .add("transferTypeDestination", "AzureStorage-PUSH");
    }

    private JsonObjectBuilder blobAddress(String containerName, String accountName, String keyName, JsonObjectBuilder... additionalProperties) {
        return Json.createObjectBuilder()
                .add("dspace:endpointType", "AzureStorage")
                .add("dspace:endpointProperties", Json.createArrayBuilder(Arrays.asList(additionalProperties))
                        .add(dspaceProperty(EDC_NAMESPACE + "container", containerName))
                        .add(dspaceProperty(EDC_NAMESPACE + "account", accountName))
                        .add(dspaceProperty(EDC_NAMESPACE + "keyName", keyName))
                );
    }

    private JsonObjectBuilder dspaceProperty(String name, String value) {
        return Json.createObjectBuilder()
                .add("dspace:name", name)
                .add("dspace:value", value);
    }
}
