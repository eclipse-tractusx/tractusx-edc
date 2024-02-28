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
import org.eclipse.edc.aws.s3.AwsClientProviderConfiguration;
import org.eclipse.edc.aws.s3.AwsClientProviderImpl;
import org.eclipse.edc.aws.s3.S3BucketSchema;
import org.eclipse.edc.aws.s3.S3ClientRequest;
import org.eclipse.edc.aws.s3.testfixtures.annotations.AwsS3IntegrationTest;
import org.eclipse.edc.azure.testfixtures.annotations.AzureStorageIntegrationTest;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.AZBLOB_CONSUMER_ACCOUNT_KEY;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.AZBLOB_CONSUMER_ACCOUNT_NAME;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.AZBLOB_CONSUMER_CONTAINER_NAME;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.AZBLOB_CONSUMER_KEY_ALIAS;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.MINIO_CONTAINER_PORT;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.MINIO_DOCKER_IMAGE;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.S3_ACCESS_KEY_ID;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.S3_CONSUMER_BUCKET_NAME;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.TESTFILE_NAME;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.blobDestinationAddress;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestFunctions.listObjects;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@Testcontainers
@AzureStorageIntegrationTest
@AwsS3IntegrationTest
public class MultiCloudTest {
    // S3 test constants
    public static final String REGION = Region.US_WEST_2.id();
    public static final String BUCKET_NAME = S3_CONSUMER_BUCKET_NAME;
    public static final String BLOB_KEY_ALIAS = AZBLOB_CONSUMER_KEY_ALIAS;
    private static final String ACCESS_KEY_ID = S3_ACCESS_KEY_ID; // user name
    private static final String SECRET_ACCESS_KEY = UUID.randomUUID().toString(); // password

    // Azure Blob test constants
    private static final String BLOB_ACCOUNT_NAME = AZBLOB_CONSUMER_ACCOUNT_NAME;
    private static final String BLOB_ACCOUNT_KEY = AZBLOB_CONSUMER_ACCOUNT_KEY;
    private static final int AZURITE_HOST_PORT = getFreePort();
    private static final String BLOB_CONTAINER_NAME = AZBLOB_CONSUMER_CONTAINER_NAME;

    // General constants, containers etc.
    private static final int PROVIDER_CONTROL_PORT = getFreePort(); // port of the control api
    @RegisterExtension
    protected static final ParticipantRuntime DATAPLANE_RUNTIME = new ParticipantRuntime(
            ":edc-tests:runtime:dataplane-cloud",
            "MultiCloud-Dataplane",
            RuntimeConfig.Azure.createDataplane("/control", PROVIDER_CONTROL_PORT, AZURITE_HOST_PORT)
    );

    @Container
    private final GenericContainer<?> s3Container = new GenericContainer<>(MINIO_DOCKER_IMAGE)
            .withEnv("MINIO_ROOT_USER", ACCESS_KEY_ID)
            .withEnv("MINIO_ROOT_PASSWORD", SECRET_ACCESS_KEY)
            .withExposedPorts(MINIO_CONTAINER_PORT);

    @Container
    private final FixedHostPortGenericContainer<?> azuriteContainer = new FixedHostPortGenericContainer<>(TestConstants.AZURITE_DOCKER_IMAGE)
            .withFixedExposedPort(AZURITE_HOST_PORT, 10000)
            .withEnv("AZURITE_ACCOUNTS", BLOB_ACCOUNT_NAME + ":" + BLOB_ACCOUNT_KEY);

    private AzureBlobHelper blobStoreHelper;
    private S3Client s3Client;
    private String s3EndpointOverride;

    @BeforeEach
    void setup() {
        blobStoreHelper = new AzureBlobHelper(BLOB_ACCOUNT_NAME, BLOB_ACCOUNT_KEY, azuriteContainer.getHost(), azuriteContainer.getMappedPort(10000));
        s3EndpointOverride = "http://localhost:%s/".formatted(s3Container.getMappedPort(MINIO_CONTAINER_PORT));
        var providerConfig = AwsClientProviderConfiguration.Builder.newInstance()
                .endpointOverride(URI.create(s3EndpointOverride))
                .credentialsProvider(() -> AwsBasicCredentials.create(ACCESS_KEY_ID, SECRET_ACCESS_KEY))
                .build();
        s3Client = new AwsClientProviderImpl(providerConfig).s3Client(S3ClientRequest.from(REGION, s3EndpointOverride));
    }

    @Test
    void transferFile_azureToS3() {
        // create container in Azure Blob, upload file
        var bcc = blobStoreHelper.createContainer(BLOB_CONTAINER_NAME);
        blobStoreHelper.uploadBlob(bcc, TESTFILE_NAME);
        DATAPLANE_RUNTIME.getVault().storeSecret(BLOB_KEY_ALIAS, BLOB_ACCOUNT_KEY);

        // create target bucket in S3
        var r = s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());
        assertThat(r.sdkHttpResponse().isSuccessful()).isTrue();

        // create data flow request
        var dfr = DataFlowStartMessage.Builder.newInstance()
                .id("test-request")
                .sourceDataAddress(DataAddress.Builder.newInstance()
                        .type("AzureStorage")
                        .property("container", BLOB_CONTAINER_NAME)
                        .property("account", BLOB_ACCOUNT_NAME)
                        .property("keyName", BLOB_KEY_ALIAS)
                        .property("blobName", TESTFILE_NAME)
                        .build()
                )
                .destinationDataAddress(DataAddress.Builder.newInstance()
                        .type(S3BucketSchema.TYPE)
                        .keyName(TESTFILE_NAME)
                        .property(S3BucketSchema.REGION, REGION)
                        .property(S3BucketSchema.BUCKET_NAME, BUCKET_NAME)
                        .property(S3BucketSchema.ACCESS_KEY_ID, ACCESS_KEY_ID)
                        .property(S3BucketSchema.SECRET_ACCESS_KEY, SECRET_ACCESS_KEY)
                        .property(S3BucketSchema.ENDPOINT_OVERRIDE, s3EndpointOverride)
                        .build()
                )
                .processId("test-process-id")
                .build();

        var url = "http://localhost:%s/control/transfer".formatted(PROVIDER_CONTROL_PORT);
        given().when()
                .baseUri(url)
                .contentType(ContentType.JSON)
                .body(dfr)
                .post()
                .then()
                .log().ifValidationFails()
                .log().ifError()
                .statusCode(200);

        await().pollInterval(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(60))
                .untilAsserted(() -> assertThat(listObjects(s3Client, BUCKET_NAME))
                        .isNotEmpty()
                        .contains(TESTFILE_NAME));
    }

    @Test
    void transferFile_s3ToAzure() {
        // create source bucket in S3, upload file
        var b1 = s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());
        assertThat(b1.sdkHttpResponse().isSuccessful()).isTrue();
        var putResponse = s3Client.putObject(PutObjectRequest.builder().bucket(BUCKET_NAME).key(TESTFILE_NAME).build(), TestUtils.getFileFromResourceName(TESTFILE_NAME).toPath());
        assertThat(putResponse.sdkHttpResponse().isSuccessful()).isTrue();


        // create container in consumer's blob store
        blobStoreHelper.createContainer(BLOB_CONTAINER_NAME);
        DATAPLANE_RUNTIME.getVault().storeSecret(BLOB_KEY_ALIAS, """
                {"sas": "%s","edctype":"dataspaceconnector:azuretoken"}
                """.formatted(blobStoreHelper.generateAccountSas(BLOB_CONTAINER_NAME)));


        // create data flow request
        var dfr = DataFlowStartMessage.Builder.newInstance()
                .id("test-request")
                .sourceDataAddress(DataAddress.Builder.newInstance()
                        .type(S3BucketSchema.TYPE)
                        .keyName(TESTFILE_NAME)
                        .property(S3BucketSchema.REGION, REGION)
                        .property(S3BucketSchema.BUCKET_NAME, BUCKET_NAME)
                        .property(S3BucketSchema.ACCESS_KEY_ID, ACCESS_KEY_ID)
                        .property(S3BucketSchema.SECRET_ACCESS_KEY, SECRET_ACCESS_KEY)
                        .property(S3BucketSchema.ENDPOINT_OVERRIDE, s3EndpointOverride)
                        .build()
                )
                .destinationDataAddress(blobDestinationAddress(TESTFILE_NAME))
                .processId("test-process-id")
                .build();

        var url = "http://localhost:%s/control/transfer".formatted(PROVIDER_CONTROL_PORT);
        given().when()
                .baseUri(url)
                .contentType(ContentType.JSON)
                .body(dfr)
                .post()
                .then()
                .statusCode(200);

        await().pollInterval(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(60))
                .untilAsserted(() -> assertThat(blobStoreHelper.listBlobs(BLOB_CONTAINER_NAME))
                        .isNotEmpty()
                        .contains(TESTFILE_NAME));
    }
}
