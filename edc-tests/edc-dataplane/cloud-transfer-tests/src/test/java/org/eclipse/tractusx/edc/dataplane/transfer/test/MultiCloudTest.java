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
import org.eclipse.edc.aws.s3.spi.S3BucketSchema;
import org.eclipse.edc.connector.controlplane.test.system.utils.LazySupplier;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.tractusx.edc.tests.aws.MinioExtension;
import org.eclipse.tractusx.edc.tests.azure.AzureBlobClient;
import org.eclipse.tractusx.edc.tests.azure.AzuriteExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage.EDC_DATA_FLOW_START_MESSAGE_TYPE;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.AZBLOB_CONSUMER_KEY_ALIAS;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.CONSUMER_AZURITE_ACCOUNT;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.PREFIX_FOR_MUTIPLE_FILES;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.TESTFILE_NAME;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestFunctions.listObjects;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@Testcontainers
@EndToEndTest
public class MultiCloudTest {

    public static final String BLOB_KEY_ALIAS = AZBLOB_CONSUMER_KEY_ALIAS;

    private static final int AZURITE_HOST_PORT = getFreePort();

    private static final LazySupplier<URI> CONTROL_API_URI = new LazySupplier<>(() -> URI.create("http://localhost:%s/control".formatted(getFreePort())));
    private static final LazySupplier<URI> START_DATAFLOW_URI = new LazySupplier<>(() -> URI.create("%s/v1/dataflows".formatted(CONTROL_API_URI.get())));

    @RegisterExtension
    protected static final RuntimeExtension DATAPLANE_RUNTIME = new RuntimePerClassExtension(
            new EmbeddedRuntime("MultiCloud-Dataplane", ":edc-tests:runtime:dataplane-cloud")
                    .configurationProvider(() -> RuntimeConfig.Azure.blobstoreDataplaneConfig(CONTROL_API_URI, AZURITE_HOST_PORT)));

    @RegisterExtension
    private static final MinioExtension MINIO_CONTAINER = new MinioExtension();

    @RegisterExtension
    private static final AzuriteExtension AZURITE_CONTAINER = new AzuriteExtension(AZURITE_HOST_PORT, CONSUMER_AZURITE_ACCOUNT);

    private AzureBlobClient blobStoreClient;
    private S3Client s3Client;

    @BeforeEach
    void setup() {
        blobStoreClient = AZURITE_CONTAINER.getClientFor(CONSUMER_AZURITE_ACCOUNT);
        s3Client = MINIO_CONTAINER.s3Client();
    }

    @Test
    void transferFile_azureToS3MultipleFiles(Vault vault) {
        var containerName = UUID.randomUUID().toString();
        var sourceContainer = blobStoreClient.createContainer(containerName);
        var filesNames = new ArrayDeque<String>();

        var fileData = BinaryData.fromString(TestUtils.getResourceFileContentAsString(TESTFILE_NAME));
        var fileNames = IntStream.rangeClosed(1, 2).mapToObj(i -> PREFIX_FOR_MUTIPLE_FILES + i + '_' + TESTFILE_NAME).toList();
        fileNames.forEach(filename -> blobStoreClient.uploadBlob(sourceContainer, fileData, filename));

        vault.storeSecret(BLOB_KEY_ALIAS, CONSUMER_AZURITE_ACCOUNT.key());

        var bucketName = UUID.randomUUID().toString();
        var destinationBucket = s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
        assertThat(destinationBucket.sdkHttpResponse().isSuccessful()).isTrue();

        var request = Json.createObjectBuilder()
                .add("@context", Json.createObjectBuilder().add("@vocab", EDC_NAMESPACE).add("dspace", "https://w3id.org/dspace/v0.8/"))
                .add("@type", EDC_DATA_FLOW_START_MESSAGE_TYPE)
                .add("@id", UUID.randomUUID().toString())
                .add("processId", UUID.randomUUID().toString())
                .add("sourceDataAddress", Json.createObjectBuilder()
                        .add("dspace:endpointType", "AzureStorage")
                        .add("dspace:endpointProperties", Json.createArrayBuilder()
                                .add(dspaceProperty(EDC_NAMESPACE + "container", containerName))
                                .add(dspaceProperty(EDC_NAMESPACE + "account", CONSUMER_AZURITE_ACCOUNT.name()))
                                .add(dspaceProperty(EDC_NAMESPACE + "keyName", BLOB_KEY_ALIAS))
                                .add(dspaceProperty(EDC_NAMESPACE + "blobPrefix", PREFIX_FOR_MUTIPLE_FILES))
                        )
                )
                .add("destinationDataAddress", Json.createObjectBuilder()
                        .add("dspace:endpointType", S3BucketSchema.TYPE)
                        .add("dspace:endpointProperties", Json.createArrayBuilder()
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.REGION, MINIO_CONTAINER.getS3region()))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.BUCKET_NAME, bucketName))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.ACCESS_KEY_ID, MINIO_CONTAINER.getCredentials().accessKeyId()))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.SECRET_ACCESS_KEY, MINIO_CONTAINER.getCredentials().secretAccessKey()))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.ENDPOINT_OVERRIDE, MINIO_CONTAINER.getEndpointOverride()))
                        )
                )
                .add("flowType", "PUSH")
                .add("transferTypeDestination", "AmazonS3-PUSH")
                .build();


        given().when()
                .baseUri(START_DATAFLOW_URI.get().toString())
                .contentType(ContentType.JSON)
                .body(request)
                .post()
                .then()
                .log().ifError()
                .statusCode(200);

        await().pollInterval(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(60))
                .untilAsserted(() -> assertThat(listObjects(s3Client, bucketName))
                        .isNotEmpty()
                        .containsAll(filesNames));
    }

    @Test
    void transferFile_azureToS3(Vault vault) {
        var fileData = BinaryData.fromString(TestUtils.getResourceFileContentAsString(TESTFILE_NAME));
        var containerName = UUID.randomUUID().toString();
        var sourceContainer = blobStoreClient.createContainer(containerName);
        blobStoreClient.uploadBlob(sourceContainer, fileData, TESTFILE_NAME);
        vault.storeSecret(BLOB_KEY_ALIAS, CONSUMER_AZURITE_ACCOUNT.key());

        var bucketName = UUID.randomUUID().toString();
        var r = s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
        assertThat(r.sdkHttpResponse().isSuccessful()).isTrue();

        var request = Json.createObjectBuilder()
                .add("@context", Json.createObjectBuilder().add("@vocab", EDC_NAMESPACE).add("dspace", "https://w3id.org/dspace/v0.8/"))
                .add("@type", EDC_DATA_FLOW_START_MESSAGE_TYPE)
                .add("@id", UUID.randomUUID().toString())
                .add("processId", UUID.randomUUID().toString())
                .add("sourceDataAddress", Json.createObjectBuilder()
                        .add("dspace:endpointType", "AzureStorage")
                        .add("dspace:endpointProperties", Json.createArrayBuilder()
                                .add(dspaceProperty(EDC_NAMESPACE + "container", containerName))
                                .add(dspaceProperty(EDC_NAMESPACE + "account", CONSUMER_AZURITE_ACCOUNT.name()))
                                .add(dspaceProperty(EDC_NAMESPACE + "keyName", BLOB_KEY_ALIAS))
                                .add(dspaceProperty(EDC_NAMESPACE + "blobName", TESTFILE_NAME))
                        )
                )
                .add("destinationDataAddress", Json.createObjectBuilder()
                        .add("dspace:endpointType", S3BucketSchema.TYPE)
                        .add("dspace:endpointProperties", Json.createArrayBuilder()
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.REGION, MINIO_CONTAINER.getS3region()))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.BUCKET_NAME, bucketName))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.ACCESS_KEY_ID, MINIO_CONTAINER.getCredentials().accessKeyId()))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.SECRET_ACCESS_KEY, MINIO_CONTAINER.getCredentials().secretAccessKey()))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.ENDPOINT_OVERRIDE, MINIO_CONTAINER.getEndpointOverride()))
                        )
                )
                .add("flowType", "PUSH")
                .add("transferTypeDestination", "AmazonS3-PUSH")
                .build();

        given().when()
                .baseUri(START_DATAFLOW_URI.get().toString())
                .contentType(ContentType.JSON)
                .body(request)
                .post()
                .then()
                .log().ifError()
                .statusCode(200);

        await().pollInterval(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(60))
                .untilAsserted(() -> assertThat(listObjects(s3Client, bucketName))
                        .isNotEmpty()
                        .contains(TESTFILE_NAME));
    }


    @Test
    void transferFile_s3ToAzureMultipleFiles(Vault vault) {
        var bucketName = UUID.randomUUID().toString();
        var sourceBucket = s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
        assertThat(sourceBucket.sdkHttpResponse().isSuccessful()).isTrue();

        var putResponse = new AtomicBoolean(true);
        var filesNames = new ArrayDeque<String>();

        var fileNames = IntStream.rangeClosed(1, 2).mapToObj(i -> PREFIX_FOR_MUTIPLE_FILES + i + '_' + TESTFILE_NAME).toList();
        fileNames.forEach(filename -> putResponse.set(s3Client.putObject(PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(filename)
                        .build(), TestUtils.getFileFromResourceName(TESTFILE_NAME).toPath())
                .sdkHttpResponse()
                .isSuccessful() && putResponse.get()));
        assertThat(putResponse.get()).isTrue();

        var containerName = UUID.randomUUID().toString();
        blobStoreClient.createContainer(containerName);
        vault.storeSecret(BLOB_KEY_ALIAS, """
                {"sas": "%s","edctype":"dataspaceconnector:azuretoken"}
                """.formatted(blobStoreClient.generateAccountSas(containerName)));

        var request = Json.createObjectBuilder()
                .add("@context", Json.createObjectBuilder().add("@vocab", EDC_NAMESPACE).add("dspace", "https://w3id.org/dspace/v0.8/"))
                .add("@type", EDC_DATA_FLOW_START_MESSAGE_TYPE)
                .add("@id", UUID.randomUUID().toString())
                .add("processId", UUID.randomUUID().toString())
                .add("sourceDataAddress", Json.createObjectBuilder()
                        .add("dspace:endpointType", S3BucketSchema.TYPE)
                        .add("dspace:endpointProperties", Json.createArrayBuilder()
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.OBJECT_PREFIX, PREFIX_FOR_MUTIPLE_FILES))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.REGION, MINIO_CONTAINER.getS3region()))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.BUCKET_NAME, bucketName))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.ACCESS_KEY_ID, MINIO_CONTAINER.getCredentials().accessKeyId()))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.SECRET_ACCESS_KEY, MINIO_CONTAINER.getCredentials().secretAccessKey()))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.ENDPOINT_OVERRIDE, MINIO_CONTAINER.getEndpointOverride()))
                        )
                )
                .add("destinationDataAddress", Json.createObjectBuilder()
                        .add("dspace:endpointType", "AzureStorage")
                        .add("dspace:endpointProperties", Json.createArrayBuilder()
                                .add(dspaceProperty(EDC_NAMESPACE + "container", containerName))
                                .add(dspaceProperty(EDC_NAMESPACE + "account", CONSUMER_AZURITE_ACCOUNT.name()))
                                .add(dspaceProperty(EDC_NAMESPACE + "keyName", AZBLOB_CONSUMER_KEY_ALIAS))
                        )
                )
                .add("flowType", "PUSH")
                .add("transferTypeDestination", "AzureStorage-PUSH")
                .build();

        given().when()
                .baseUri(START_DATAFLOW_URI.get().toString())
                .contentType(ContentType.JSON)
                .body(request)
                .post()
                .then()
                .statusCode(200);

        await().pollInterval(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(60))
                .untilAsserted(() -> assertThat(blobStoreClient.listBlobs(containerName))
                        .isNotEmpty()
                        .containsAll(filesNames));
    }

    @Test
    void transferFile_s3ToAzureMultipleFiles_whenConsumerDefinesBloblName_success(Vault vault) {
        var bucketName = UUID.randomUUID().toString();
        var sourceBucket = s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
        assertThat(sourceBucket.sdkHttpResponse().isSuccessful()).isTrue();

        var putResponse = new AtomicBoolean(true);
        var filesNames = new ArrayDeque<String>();

        var fileNames = IntStream.rangeClosed(1, 2).mapToObj(i -> PREFIX_FOR_MUTIPLE_FILES + i + '_' + TESTFILE_NAME).toList();
        fileNames.forEach(filename -> putResponse.set(s3Client.putObject(PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(filename)
                        .build(), TestUtils.getFileFromResourceName(TESTFILE_NAME).toPath())
                .sdkHttpResponse()
                .isSuccessful() && putResponse.get()));

        assertThat(putResponse.get()).isTrue();

        var containerName = UUID.randomUUID().toString();
        blobStoreClient.createContainer(containerName);
        vault.storeSecret(BLOB_KEY_ALIAS, """
                {"sas": "%s","edctype":"dataspaceconnector:azuretoken"}
                """.formatted(blobStoreClient.generateAccountSas(containerName)));

        var request = Json.createObjectBuilder()
                .add("@context", Json.createObjectBuilder().add("@vocab", EDC_NAMESPACE).add("dspace", "https://w3id.org/dspace/v0.8/"))
                .add("@type", EDC_DATA_FLOW_START_MESSAGE_TYPE)
                .add("@id", UUID.randomUUID().toString())
                .add("processId", UUID.randomUUID().toString())
                .add("sourceDataAddress", Json.createObjectBuilder()
                        .add("dspace:endpointType", S3BucketSchema.TYPE)
                        .add("dspace:endpointProperties", Json.createArrayBuilder()
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.OBJECT_PREFIX, PREFIX_FOR_MUTIPLE_FILES))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.REGION, MINIO_CONTAINER.getS3region()))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.BUCKET_NAME, bucketName))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.ACCESS_KEY_ID, MINIO_CONTAINER.getCredentials().accessKeyId()))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.SECRET_ACCESS_KEY, MINIO_CONTAINER.getCredentials().secretAccessKey()))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.ENDPOINT_OVERRIDE, MINIO_CONTAINER.getEndpointOverride()))
                        )
                )
                .add("destinationDataAddress", Json.createObjectBuilder()
                        .add("dspace:endpointType", "AzureStorage")
                        .add("dspace:endpointProperties", Json.createArrayBuilder()
                                .add(dspaceProperty(EDC_NAMESPACE + "container", containerName))
                                .add(dspaceProperty(EDC_NAMESPACE + "account", CONSUMER_AZURITE_ACCOUNT.name()))
                                .add(dspaceProperty(EDC_NAMESPACE + "keyName", AZBLOB_CONSUMER_KEY_ALIAS))
                                .add(dspaceProperty(EDC_NAMESPACE + "blobName", "NOME_TEST"))
                        )
                )
                .add("flowType", "PUSH")
                .add("transferTypeDestination", "AzureStorage-PUSH")
                .build();


        given().when()
                .baseUri(START_DATAFLOW_URI.get().toString())
                .contentType(ContentType.JSON)
                .body(request)
                .post()
                .then()
                .statusCode(200);

        await().pollInterval(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(60))
                .untilAsserted(() -> assertThat(blobStoreClient.listBlobs(containerName))
                        .isNotEmpty()
                        .containsAll(filesNames));
    }

    @Test
    void transferFile_s3ToAzure(Vault vault) {
        var bucketName = UUID.randomUUID().toString();
        var b1 = s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
        assertThat(b1.sdkHttpResponse().isSuccessful()).isTrue();
        var putResponse = s3Client.putObject(PutObjectRequest.builder().bucket(bucketName).key(TESTFILE_NAME).build(), TestUtils.getFileFromResourceName(TESTFILE_NAME).toPath());
        assertThat(putResponse.sdkHttpResponse().isSuccessful()).isTrue();

        var containerName = UUID.randomUUID().toString();
        blobStoreClient.createContainer(containerName);
        vault.storeSecret(BLOB_KEY_ALIAS, """
                {"sas": "%s","edctype":"dataspaceconnector:azuretoken"}
                """.formatted(blobStoreClient.generateAccountSas(containerName)));

        var request = Json.createObjectBuilder()
                .add("@context", Json.createObjectBuilder().add("@vocab", EDC_NAMESPACE).add("dspace", "https://w3id.org/dspace/v0.8/"))
                .add("@type", EDC_DATA_FLOW_START_MESSAGE_TYPE)
                .add("@id", UUID.randomUUID().toString())
                .add("processId", UUID.randomUUID().toString())
                .add("sourceDataAddress", Json.createObjectBuilder()
                        .add("dspace:endpointType", S3BucketSchema.TYPE)
                        .add("dspace:endpointProperties", Json.createArrayBuilder()
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.OBJECT_NAME, TESTFILE_NAME))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.REGION, MINIO_CONTAINER.getS3region()))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.BUCKET_NAME, bucketName))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.ACCESS_KEY_ID, MINIO_CONTAINER.getCredentials().accessKeyId()))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.SECRET_ACCESS_KEY, MINIO_CONTAINER.getCredentials().secretAccessKey()))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.ENDPOINT_OVERRIDE, MINIO_CONTAINER.getEndpointOverride()))
                        )
                )
                .add("destinationDataAddress", Json.createObjectBuilder()
                        .add("dspace:endpointType", "AzureStorage")
                        .add("dspace:endpointProperties", Json.createArrayBuilder()
                                .add(dspaceProperty(EDC_NAMESPACE + "container", containerName))
                                .add(dspaceProperty(EDC_NAMESPACE + "account", CONSUMER_AZURITE_ACCOUNT.name()))
                                .add(dspaceProperty(EDC_NAMESPACE + "keyName", AZBLOB_CONSUMER_KEY_ALIAS))
                                .add(dspaceProperty(EDC_NAMESPACE + "blobName", TESTFILE_NAME))
                        )
                )
                .add("flowType", "PUSH")
                .add("transferTypeDestination", "AzureStorage-PUSH")
                .build();

        given().when()
                .baseUri(START_DATAFLOW_URI.get().toString())
                .contentType(ContentType.JSON)
                .body(request)
                .post()
                .then()
                .statusCode(200);

        await().pollInterval(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(60))
                .untilAsserted(() -> assertThat(blobStoreClient.listBlobs(containerName))
                        .isNotEmpty()
                        .contains(TESTFILE_NAME));
    }

    private static JsonObjectBuilder dspaceProperty(String name, String value) {
        return Json.createObjectBuilder()
                .add("dspace:name", name)
                .add("dspace:value", value);
    }

}
