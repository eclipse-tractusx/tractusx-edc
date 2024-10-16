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
import org.eclipse.edc.aws.s3.AwsClientProviderConfiguration;
import org.eclipse.edc.aws.s3.AwsClientProviderImpl;
import org.eclipse.edc.aws.s3.S3ClientRequest;
import org.eclipse.edc.aws.s3.spi.S3BucketSchema;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.security.Vault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.regions.Region;
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
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.AZBLOB_CONSUMER_CONTAINER_NAME;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.AZBLOB_CONSUMER_KEY_ALIAS;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.CONSUMER_AZURITE_ACCOUNT;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.PREFIX_FOR_MUTIPLE_FILES;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.S3_CONSUMER_BUCKET_NAME;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.TESTFILE_NAME;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestFunctions.listObjects;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@Testcontainers
@CloudTransferTest
public class MultiCloudTest {
    // S3 test constants
    public static final String REGION = Region.US_WEST_2.id();
    public static final String BUCKET_NAME = S3_CONSUMER_BUCKET_NAME;
    public static final String BLOB_KEY_ALIAS = AZBLOB_CONSUMER_KEY_ALIAS;

    private static final int AZURITE_HOST_PORT = getFreePort();
    private static final String BLOB_CONTAINER_NAME = AZBLOB_CONSUMER_CONTAINER_NAME;

    // General constants, containers etc.
    private static final int PROVIDER_CONTROL_PORT = getFreePort(); // port of the control api
    private static final String START_DATAFLOW_URL = "http://localhost:%s/control/v1/dataflows".formatted(PROVIDER_CONTROL_PORT);
    @RegisterExtension
    protected static final RuntimeExtension DATAPLANE_RUNTIME = new RuntimePerClassExtension(new EmbeddedRuntime(
            "MultiCloud-Dataplane",
            RuntimeConfig.Azure.blobstoreDataplaneConfig("/control", PROVIDER_CONTROL_PORT, AZURITE_HOST_PORT),
            ":edc-tests:runtime:dataplane-cloud"
    ));

    @Container
    private final MinioContainer s3Container = new MinioContainer();

    @Container
    private final AzuriteContainer azuriteContainer = new AzuriteContainer(AZURITE_HOST_PORT, CONSUMER_AZURITE_ACCOUNT);

    private AzureBlobHelper blobStoreHelper;
    private S3Client s3Client;
    private String s3EndpointOverride;

    @BeforeEach
    void setup() {
        blobStoreHelper = azuriteContainer.getHelper(CONSUMER_AZURITE_ACCOUNT);
        s3EndpointOverride = "http://localhost:%s/".formatted(s3Container.getFirstMappedPort());
        var providerConfig = AwsClientProviderConfiguration.Builder.newInstance()
                .endpointOverride(URI.create(s3EndpointOverride))
                .credentialsProvider(s3Container::getCredentials)
                .build();
        s3Client = new AwsClientProviderImpl(providerConfig).s3Client(S3ClientRequest.from(REGION, s3EndpointOverride));
    }

    @Test
    void transferFile_azureToS3MultipleFiles(Vault vault) {
        var sourceContainer = blobStoreHelper.createContainer(BLOB_CONTAINER_NAME);
        var filesNames = new ArrayDeque<String>();

        var fileData = BinaryData.fromString(TestUtils.getResourceFileContentAsString(TESTFILE_NAME));
        var fileNames = IntStream.rangeClosed(1, 2).mapToObj(i -> PREFIX_FOR_MUTIPLE_FILES + i + '_' + TESTFILE_NAME).toList();
        fileNames.forEach(filename -> blobStoreHelper.uploadBlob(sourceContainer, fileData, filename));

        vault.storeSecret(BLOB_KEY_ALIAS, CONSUMER_AZURITE_ACCOUNT.key());

        var destinationBucket = s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());
        assertThat(destinationBucket.sdkHttpResponse().isSuccessful()).isTrue();

        var request = Json.createObjectBuilder()
                .add("@context", Json.createObjectBuilder().add("@vocab", EDC_NAMESPACE).add("dspace", "https://w3id.org/dspace/v0.8/"))
                .add("@type", EDC_DATA_FLOW_START_MESSAGE_TYPE)
                .add("@id", UUID.randomUUID().toString())
                .add("processId", UUID.randomUUID().toString())
                .add("sourceDataAddress", Json.createObjectBuilder()
                        .add("dspace:endpointType", "AzureStorage")
                        .add("dspace:endpointProperties", Json.createArrayBuilder()
                                .add(dspaceProperty(EDC_NAMESPACE + "container", BLOB_CONTAINER_NAME))
                                .add(dspaceProperty(EDC_NAMESPACE + "account", CONSUMER_AZURITE_ACCOUNT.name()))
                                .add(dspaceProperty(EDC_NAMESPACE + "keyName", BLOB_KEY_ALIAS))
                                .add(dspaceProperty(EDC_NAMESPACE + "blobPrefix", PREFIX_FOR_MUTIPLE_FILES))
                        )
                )
                .add("destinationDataAddress", Json.createObjectBuilder()
                        .add("dspace:endpointType", S3BucketSchema.TYPE)
                        .add("dspace:endpointProperties", Json.createArrayBuilder()
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.REGION, REGION))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.BUCKET_NAME, BUCKET_NAME))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.ACCESS_KEY_ID, s3Container.getCredentials().accessKeyId()))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.SECRET_ACCESS_KEY, s3Container.getCredentials().secretAccessKey()))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.ENDPOINT_OVERRIDE, s3EndpointOverride))
                        )
                )
                .add("flowType", "PUSH")
                .add("transferTypeDestination", "AmazonS3-PUSH")
                .build();


        given().when()
                .baseUri(START_DATAFLOW_URL)
                .contentType(ContentType.JSON)
                .body(request)
                .post()
                .then()
                .log().ifError()
                .statusCode(200);

        await().pollInterval(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(60))
                .untilAsserted(() -> assertThat(listObjects(s3Client, BUCKET_NAME))
                        .isNotEmpty()
                        .containsAll(filesNames));
    }

    @Test
    void transferFile_azureToS3(Vault vault) {
        var fileData = BinaryData.fromString(TestUtils.getResourceFileContentAsString(TESTFILE_NAME));
        var sourceContainer = blobStoreHelper.createContainer(BLOB_CONTAINER_NAME);
        blobStoreHelper.uploadBlob(sourceContainer, fileData, TESTFILE_NAME);
        vault.storeSecret(BLOB_KEY_ALIAS, CONSUMER_AZURITE_ACCOUNT.key());

        var r = s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());
        assertThat(r.sdkHttpResponse().isSuccessful()).isTrue();

        var request = Json.createObjectBuilder()
                .add("@context", Json.createObjectBuilder().add("@vocab", EDC_NAMESPACE).add("dspace", "https://w3id.org/dspace/v0.8/"))
                .add("@type", EDC_DATA_FLOW_START_MESSAGE_TYPE)
                .add("@id", UUID.randomUUID().toString())
                .add("processId", UUID.randomUUID().toString())
                .add("sourceDataAddress", Json.createObjectBuilder()
                        .add("dspace:endpointType", "AzureStorage")
                        .add("dspace:endpointProperties", Json.createArrayBuilder()
                                .add(dspaceProperty(EDC_NAMESPACE + "container", BLOB_CONTAINER_NAME))
                                .add(dspaceProperty(EDC_NAMESPACE + "account", CONSUMER_AZURITE_ACCOUNT.name()))
                                .add(dspaceProperty(EDC_NAMESPACE + "keyName", BLOB_KEY_ALIAS))
                                .add(dspaceProperty(EDC_NAMESPACE + "blobName", TESTFILE_NAME))
                        )
                )
                .add("destinationDataAddress", Json.createObjectBuilder()
                        .add("dspace:endpointType", S3BucketSchema.TYPE)
                        .add("dspace:endpointProperties", Json.createArrayBuilder()
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.REGION, REGION))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.BUCKET_NAME, BUCKET_NAME))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.ACCESS_KEY_ID, s3Container.getCredentials().accessKeyId()))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.SECRET_ACCESS_KEY, s3Container.getCredentials().secretAccessKey()))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.ENDPOINT_OVERRIDE, s3EndpointOverride))
                        )
                )
                .add("flowType", "PUSH")
                .add("transferTypeDestination", "AmazonS3-PUSH")
                .build();

        given().when()
                .baseUri(START_DATAFLOW_URL)
                .contentType(ContentType.JSON)
                .body(request)
                .post()
                .then()
                .log().ifError()
                .statusCode(200);

        await().pollInterval(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(60))
                .untilAsserted(() -> assertThat(listObjects(s3Client, BUCKET_NAME))
                        .isNotEmpty()
                        .contains(TESTFILE_NAME));
    }


    @Test
    void transferFile_s3ToAzureMultipleFiles(Vault vault) {
        var sourceBucket = s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());
        assertThat(sourceBucket.sdkHttpResponse().isSuccessful()).isTrue();

        var putResponse = new AtomicBoolean(true);
        var filesNames = new ArrayDeque<String>();

        var fileNames = IntStream.rangeClosed(1, 2).mapToObj(i -> PREFIX_FOR_MUTIPLE_FILES + i + '_' + TESTFILE_NAME).toList();
        fileNames.forEach(filename -> putResponse.set(s3Client.putObject(PutObjectRequest.builder()
                        .bucket(BUCKET_NAME)
                        .key(filename)
                        .build(), TestUtils.getFileFromResourceName(TESTFILE_NAME).toPath())
                .sdkHttpResponse()
                .isSuccessful() && putResponse.get()));
        assertThat(putResponse.get()).isTrue();

        blobStoreHelper.createContainer(BLOB_CONTAINER_NAME);
        vault.storeSecret(BLOB_KEY_ALIAS, """
                {"sas": "%s","edctype":"dataspaceconnector:azuretoken"}
                """.formatted(blobStoreHelper.generateAccountSas(BLOB_CONTAINER_NAME)));

        var request = Json.createObjectBuilder()
                .add("@context", Json.createObjectBuilder().add("@vocab", EDC_NAMESPACE).add("dspace", "https://w3id.org/dspace/v0.8/"))
                .add("@type", EDC_DATA_FLOW_START_MESSAGE_TYPE)
                .add("@id", UUID.randomUUID().toString())
                .add("processId", UUID.randomUUID().toString())
                .add("sourceDataAddress", Json.createObjectBuilder()
                        .add("dspace:endpointType", S3BucketSchema.TYPE)
                        .add("dspace:endpointProperties", Json.createArrayBuilder()
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.OBJECT_PREFIX, PREFIX_FOR_MUTIPLE_FILES))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.REGION, REGION))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.BUCKET_NAME, BUCKET_NAME))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.ACCESS_KEY_ID, s3Container.getCredentials().accessKeyId()))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.SECRET_ACCESS_KEY, s3Container.getCredentials().secretAccessKey()))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.ENDPOINT_OVERRIDE, s3EndpointOverride))
                        )
                )
                .add("destinationDataAddress", Json.createObjectBuilder()
                        .add("dspace:endpointType", "AzureStorage")
                        .add("dspace:endpointProperties", Json.createArrayBuilder()
                                .add(dspaceProperty(EDC_NAMESPACE + "container", AZBLOB_CONSUMER_CONTAINER_NAME))
                                .add(dspaceProperty(EDC_NAMESPACE + "account", CONSUMER_AZURITE_ACCOUNT.name()))
                                .add(dspaceProperty(EDC_NAMESPACE + "keyName", AZBLOB_CONSUMER_KEY_ALIAS))
                        )
                )
                .add("flowType", "PUSH")
                .add("transferTypeDestination", "AzureStorage-PUSH")
                .build();

        given().when()
                .baseUri(START_DATAFLOW_URL)
                .contentType(ContentType.JSON)
                .body(request)
                .post()
                .then()
                .statusCode(200);

        await().pollInterval(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(60))
                .untilAsserted(() -> assertThat(blobStoreHelper.listBlobs(BLOB_CONTAINER_NAME))
                        .isNotEmpty()
                        .containsAll(filesNames));
    }

    @Test
    void transferFile_s3ToAzureMultipleFiles_whenConsumerDefinesBloblName_success(Vault vault) {
        var sourceBucket = s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());
        assertThat(sourceBucket.sdkHttpResponse().isSuccessful()).isTrue();

        var putResponse = new AtomicBoolean(true);
        var filesNames = new ArrayDeque<String>();

        var fileNames = IntStream.rangeClosed(1, 2).mapToObj(i -> PREFIX_FOR_MUTIPLE_FILES + i + '_' + TESTFILE_NAME).toList();
        fileNames.forEach(filename -> putResponse.set(s3Client.putObject(PutObjectRequest.builder()
                        .bucket(BUCKET_NAME)
                        .key(filename)
                        .build(), TestUtils.getFileFromResourceName(TESTFILE_NAME).toPath())
                .sdkHttpResponse()
                .isSuccessful() && putResponse.get()));

        assertThat(putResponse.get()).isTrue();

        blobStoreHelper.createContainer(BLOB_CONTAINER_NAME);
        vault.storeSecret(BLOB_KEY_ALIAS, """
                {"sas": "%s","edctype":"dataspaceconnector:azuretoken"}
                """.formatted(blobStoreHelper.generateAccountSas(BLOB_CONTAINER_NAME)));

        var request = Json.createObjectBuilder()
                .add("@context", Json.createObjectBuilder().add("@vocab", EDC_NAMESPACE).add("dspace", "https://w3id.org/dspace/v0.8/"))
                .add("@type", EDC_DATA_FLOW_START_MESSAGE_TYPE)
                .add("@id", UUID.randomUUID().toString())
                .add("processId", UUID.randomUUID().toString())
                .add("sourceDataAddress", Json.createObjectBuilder()
                        .add("dspace:endpointType", S3BucketSchema.TYPE)
                        .add("dspace:endpointProperties", Json.createArrayBuilder()
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.OBJECT_PREFIX, PREFIX_FOR_MUTIPLE_FILES))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.REGION, REGION))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.BUCKET_NAME, BUCKET_NAME))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.ACCESS_KEY_ID, s3Container.getCredentials().accessKeyId()))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.SECRET_ACCESS_KEY, s3Container.getCredentials().secretAccessKey()))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.ENDPOINT_OVERRIDE, s3EndpointOverride))
                        )
                )
                .add("destinationDataAddress", Json.createObjectBuilder()
                        .add("dspace:endpointType", "AzureStorage")
                        .add("dspace:endpointProperties", Json.createArrayBuilder()
                                .add(dspaceProperty(EDC_NAMESPACE + "container", AZBLOB_CONSUMER_CONTAINER_NAME))
                                .add(dspaceProperty(EDC_NAMESPACE + "account", CONSUMER_AZURITE_ACCOUNT.name()))
                                .add(dspaceProperty(EDC_NAMESPACE + "keyName", AZBLOB_CONSUMER_KEY_ALIAS))
                                .add(dspaceProperty(EDC_NAMESPACE + "blobName", "NOME_TEST"))
                        )
                )
                .add("flowType", "PUSH")
                .add("transferTypeDestination", "AzureStorage-PUSH")
                .build();


        given().when()
                .baseUri(START_DATAFLOW_URL)
                .contentType(ContentType.JSON)
                .body(request)
                .post()
                .then()
                .statusCode(200);

        await().pollInterval(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(60))
                .untilAsserted(() -> assertThat(blobStoreHelper.listBlobs(BLOB_CONTAINER_NAME))
                        .isNotEmpty()
                        .containsAll(filesNames));
    }

    @Test
    void transferFile_s3ToAzure(Vault vault) {
        var b1 = s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());
        assertThat(b1.sdkHttpResponse().isSuccessful()).isTrue();
        var putResponse = s3Client.putObject(PutObjectRequest.builder().bucket(BUCKET_NAME).key(TESTFILE_NAME).build(), TestUtils.getFileFromResourceName(TESTFILE_NAME).toPath());
        assertThat(putResponse.sdkHttpResponse().isSuccessful()).isTrue();

        blobStoreHelper.createContainer(BLOB_CONTAINER_NAME);
        vault.storeSecret(BLOB_KEY_ALIAS, """
                {"sas": "%s","edctype":"dataspaceconnector:azuretoken"}
                """.formatted(blobStoreHelper.generateAccountSas(BLOB_CONTAINER_NAME)));

        var request = Json.createObjectBuilder()
                .add("@context", Json.createObjectBuilder().add("@vocab", EDC_NAMESPACE).add("dspace", "https://w3id.org/dspace/v0.8/"))
                .add("@type", EDC_DATA_FLOW_START_MESSAGE_TYPE)
                .add("@id", UUID.randomUUID().toString())
                .add("processId", UUID.randomUUID().toString())
                .add("sourceDataAddress", Json.createObjectBuilder()
                        .add("dspace:endpointType", S3BucketSchema.TYPE)
                        .add("dspace:endpointProperties", Json.createArrayBuilder()
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.OBJECT_NAME, TESTFILE_NAME))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.REGION, REGION))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.BUCKET_NAME, BUCKET_NAME))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.ACCESS_KEY_ID, s3Container.getCredentials().accessKeyId()))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.SECRET_ACCESS_KEY, s3Container.getCredentials().secretAccessKey()))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.ENDPOINT_OVERRIDE, s3EndpointOverride))
                        )
                )
                .add("destinationDataAddress", Json.createObjectBuilder()
                        .add("dspace:endpointType", "AzureStorage")
                        .add("dspace:endpointProperties", Json.createArrayBuilder()
                                .add(dspaceProperty(EDC_NAMESPACE + "container", AZBLOB_CONSUMER_CONTAINER_NAME))
                                .add(dspaceProperty(EDC_NAMESPACE + "account", CONSUMER_AZURITE_ACCOUNT.name()))
                                .add(dspaceProperty(EDC_NAMESPACE + "keyName", AZBLOB_CONSUMER_KEY_ALIAS))
                                .add(dspaceProperty(EDC_NAMESPACE + "blobName", TESTFILE_NAME))
                        )
                )
                .add("flowType", "PUSH")
                .add("transferTypeDestination", "AzureStorage-PUSH")
                .build();

        given().when()
                .baseUri(START_DATAFLOW_URL)
                .contentType(ContentType.JSON)
                .body(request)
                .post()
                .then()
                .statusCode(200);

        await().pollInterval(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(60))
                .untilAsserted(() -> assertThat(blobStoreHelper.listBlobs(BLOB_CONTAINER_NAME))
                        .isNotEmpty()
                        .contains(TESTFILE_NAME));
    }

    private static JsonObjectBuilder dspaceProperty(String name, String value) {
        return Json.createObjectBuilder()
                .add("dspace:name", name)
                .add("dspace:value", value);
    }

}
