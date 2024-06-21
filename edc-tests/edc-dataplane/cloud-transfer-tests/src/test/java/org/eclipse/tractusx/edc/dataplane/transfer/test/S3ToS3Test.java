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
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedUpload;
import software.amazon.awssdk.transfer.s3.model.UploadRequest;

import java.io.File;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.MINIO_CONTAINER_PORT;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.MINIO_DOCKER_IMAGE;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.PREFIX_FOR_MUTIPLE_FILES;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.S3_ACCESS_KEY_ID;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.S3_CONSUMER_BUCKET_NAME;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.S3_PROVIDER_BUCKET_NAME;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.S3_REGION;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.TESTFILE_NAME;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestFunctions.createSparseFile;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestFunctions.listObjects;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

/**
 * This test is intended to verify transfers within the same cloud provider, i.e. S3-to-S3.
 * It spins up a fully-fledged dataplane and issues the DataFlowStartMessage via the data plane's Control API
 */
@Testcontainers
@CloudTransferTest
public class S3ToS3Test {
    private static final String SECRET_ACCESS_KEY = UUID.randomUUID().toString(); // password
    private static final int PROVIDER_CONTROL_PORT = getFreePort(); // port of the control api
    @RegisterExtension
    protected static final ParticipantRuntime DATAPLANE_RUNTIME = new ParticipantRuntime(
            ":edc-tests:runtime:dataplane-cloud",
            "AwsS3-Dataplane",
            RuntimeConfig.S3.s3dataplaneConfig("/control", PROVIDER_CONTROL_PORT)
    );
    @Container
    private final GenericContainer<?> providerContainer = new GenericContainer<>(MINIO_DOCKER_IMAGE)
            .withEnv("MINIO_ROOT_USER", S3_ACCESS_KEY_ID)
            .withEnv("MINIO_ROOT_PASSWORD", SECRET_ACCESS_KEY)
            .withExposedPorts(MINIO_CONTAINER_PORT);

    @Container
    private final GenericContainer<?> consumerContainer = new GenericContainer<>(MINIO_DOCKER_IMAGE)
            .withEnv("MINIO_ROOT_USER", S3_ACCESS_KEY_ID)
            .withEnv("MINIO_ROOT_PASSWORD", SECRET_ACCESS_KEY)
            .withExposedPorts(MINIO_CONTAINER_PORT);
    private S3Client providerClient;
    private S3Client consumerClient;
    private String providerEndpointOverride;
    private String consumerEndpointOverride;

    @BeforeEach
    void setup() {
        providerEndpointOverride = "http://localhost:%s/".formatted(providerContainer.getMappedPort(MINIO_CONTAINER_PORT));
        var providerConfig = AwsClientProviderConfiguration.Builder.newInstance()
                .endpointOverride(URI.create(providerEndpointOverride))
                .credentialsProvider(() -> AwsBasicCredentials.create(S3_ACCESS_KEY_ID, SECRET_ACCESS_KEY))
                .build();
        providerClient = new AwsClientProviderImpl(providerConfig).s3Client(S3ClientRequest.from(S3_REGION, providerEndpointOverride));

        consumerEndpointOverride = "http://localhost:%s".formatted(consumerContainer.getMappedPort(MINIO_CONTAINER_PORT));
        var consumerConfig = AwsClientProviderConfiguration.Builder.newInstance()
                .endpointOverride(URI.create(consumerEndpointOverride))
                .credentialsProvider(() -> AwsBasicCredentials.create(S3_ACCESS_KEY_ID, SECRET_ACCESS_KEY))
                .build();
        consumerClient = new AwsClientProviderImpl(consumerConfig).s3Client(S3ClientRequest.from(S3_REGION, consumerEndpointOverride));
    }


    @Test
    void transferMultipleFiles() {
        var sourceBucket = providerClient.createBucket(CreateBucketRequest.builder().bucket(S3_PROVIDER_BUCKET_NAME).build());
        assertThat(sourceBucket.sdkHttpResponse().isSuccessful()).isTrue();
        var putResponse = new AtomicBoolean(true);
        var filesNames = new ArrayDeque<String>();

        var fileNames = IntStream.rangeClosed(1, 2).mapToObj(i -> PREFIX_FOR_MUTIPLE_FILES + i + '_' + TESTFILE_NAME).toList();
        fileNames.forEach(filename -> putResponse.set(providerClient.putObject(PutObjectRequest.builder()
                        .bucket(S3_PROVIDER_BUCKET_NAME)
                        .key(filename)
                        .build(), TestUtils.getFileFromResourceName(TESTFILE_NAME).toPath())
                .sdkHttpResponse()
                .isSuccessful() && putResponse.get()));


        assertThat(putResponse.get()).isTrue();

        var destinationBucket = consumerClient.createBucket(CreateBucketRequest.builder().bucket(S3_CONSUMER_BUCKET_NAME).build());
        assertThat(destinationBucket.sdkHttpResponse().isSuccessful()).isTrue();
        var request = createMultipleFileFlowRequest();
        var url = "http://localhost:%s/control/transfer".formatted(PROVIDER_CONTROL_PORT);

        given().when()
                .baseUri(url)
                .contentType(ContentType.JSON)
                .body(request)
                .post()
                .then()
                .statusCode(200);

        await().pollInterval(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(60))
                .untilAsserted(() ->
                        assertThat(listObjects(consumerClient, S3_CONSUMER_BUCKET_NAME))
                                .isNotEmpty()
                                .containsAll(filesNames));
    }


    @Test
    void transferFile_success() {

        // create bucket in provider
        var b1 = providerClient.createBucket(CreateBucketRequest.builder().bucket(S3_PROVIDER_BUCKET_NAME).build());
        assertThat(b1.sdkHttpResponse().isSuccessful()).isTrue();
        // upload test file in provider
        var putResponse = providerClient.putObject(PutObjectRequest.builder().bucket(S3_PROVIDER_BUCKET_NAME).key(TESTFILE_NAME).build(), TestUtils.getFileFromResourceName(TESTFILE_NAME).toPath());
        assertThat(putResponse.sdkHttpResponse().isSuccessful()).isTrue();

        // create bucket in consumer
        var b2 = consumerClient.createBucket(CreateBucketRequest.builder().bucket(S3_CONSUMER_BUCKET_NAME).build());
        assertThat(b2.sdkHttpResponse().isSuccessful()).isTrue();

        // initiate data flow request
        var request = createFlowRequest();
        var url = "http://localhost:%s/control/transfer".formatted(PROVIDER_CONTROL_PORT);

        given().when()
                .baseUri(url)
                .contentType(ContentType.JSON)
                .body(request)
                .post()
                .then()
                .statusCode(200);


        await().pollInterval(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(60))
                .untilAsserted(() -> assertThat(listObjects(consumerClient, S3_CONSUMER_BUCKET_NAME))
                        .isNotEmpty()
                        .contains(TESTFILE_NAME));
    }

    @Test
    void transferFile_targetContainerNotExist_shouldFail() {
        // create bucket in provider
        var b1 = providerClient.createBucket(CreateBucketRequest.builder().bucket(S3_PROVIDER_BUCKET_NAME).build());
        assertThat(b1.sdkHttpResponse().isSuccessful()).isTrue();
        // upload test file in provider
        var putResponse = providerClient.putObject(PutObjectRequest.builder().bucket(S3_PROVIDER_BUCKET_NAME).key(TESTFILE_NAME).build(), TestUtils.getFileFromResourceName(TESTFILE_NAME).toPath());
        assertThat(putResponse.sdkHttpResponse().isSuccessful()).isTrue();

        // do not create bucket in consumer -> will fail!

        // initiate data flow request
        var request = createFlowRequest();
        var url = "http://localhost:%s/control/transfer".formatted(PROVIDER_CONTROL_PORT);

        given().when()
                .baseUri(url)
                .contentType(ContentType.JSON)
                .body(request)
                .post()
                .then()
                .statusCode(200);


        // wait until the data plane logs an exception that it cannot transfer the file
        await().pollInterval(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> verify(DATAPLANE_RUNTIME.getService(Monitor.class)).severe(startsWith("Failed to upload the %s object: The specified bucket does not exist".formatted(TESTFILE_NAME)),
                        isA(NoSuchBucketException.class)));
    }

    @ParameterizedTest(name = "File size bytes: {0}")
    // 1mb, 512mb, 1gb
    @ValueSource(longs = { 1024 * 1024 * 512, 1024L * 1024L * 1024L, 1024L * 1024L * 1024L * 1024 })
    void transferfile_largeFile(long sizeBytes) {

        // create large sparse file
        var file = createSparseFile(sizeBytes);

        // create bucket in provider
        var b1 = providerClient.createBucket(CreateBucketRequest.builder().bucket(S3_PROVIDER_BUCKET_NAME).build());
        assertThat(b1.sdkHttpResponse().isSuccessful()).isTrue();
        // upload test file in provider
        uploadLargeFile(file, S3_PROVIDER_BUCKET_NAME, TESTFILE_NAME).thenAccept(completedUpload -> {
            // create bucket in consumer
            var b2 = consumerClient.createBucket(CreateBucketRequest.builder().bucket(S3_CONSUMER_BUCKET_NAME).build());
            assertThat(b2.sdkHttpResponse().isSuccessful()).isTrue();

            // initiate data flow request
            var request = createFlowRequest();
            var url = "http://localhost:%s/control/transfer".formatted(PROVIDER_CONTROL_PORT);

            given().when()
                    .baseUri(url)
                    .contentType(ContentType.JSON)
                    .body(request)
                    .post()
                    .then()
                    .statusCode(200);


            await().pollInterval(Duration.ofSeconds(2))
                    .atMost(Duration.ofSeconds(60))
                    .untilAsserted(() -> assertThat(listObjects(consumerClient, S3_CONSUMER_BUCKET_NAME))
                            .isNotEmpty()
                            .contains(TESTFILE_NAME));
        });


    }

    private CompletableFuture<CompletedUpload> uploadLargeFile(File file, String bucketName, String fileName) {

        var providerConfig = AwsClientProviderConfiguration.Builder.newInstance()
                .endpointOverride(URI.create(providerEndpointOverride))
                .credentialsProvider(() -> AwsBasicCredentials.create(S3_ACCESS_KEY_ID, SECRET_ACCESS_KEY))
                .build();
        var asyncClient = new AwsClientProviderImpl(providerConfig).s3AsyncClient(S3_REGION);
        var tm = S3TransferManager.builder()
                .s3Client(asyncClient)
                .build();

        // TransferManager processes all transfers asynchronously,
        // so this call returns immediately.
        var upload = tm.upload(UploadRequest.builder()
                .putObjectRequest(PutObjectRequest.builder().key(fileName).bucket(bucketName).build())
                .requestBody(AsyncRequestBody.fromFile(file))
                .build());

        return upload.completionFuture();
    }


    private DataFlowStartMessage createFlowRequest() {
        return DataFlowStartMessage.Builder.newInstance()
                .id("test-request")
                .sourceDataAddress(DataAddress.Builder.newInstance()
                        .type(S3BucketSchema.TYPE)
                        .keyName(TESTFILE_NAME)
                        .property(S3BucketSchema.REGION, S3_REGION)
                        .property(S3BucketSchema.BUCKET_NAME, S3_PROVIDER_BUCKET_NAME)
                        .property(S3BucketSchema.ACCESS_KEY_ID, S3_ACCESS_KEY_ID)
                        .property(S3BucketSchema.SECRET_ACCESS_KEY, SECRET_ACCESS_KEY)
                        .property(S3BucketSchema.ENDPOINT_OVERRIDE, providerEndpointOverride)
                        .build()
                )
                .destinationDataAddress(DataAddress.Builder.newInstance()
                        .type(S3BucketSchema.TYPE)
                        .keyName(TESTFILE_NAME)
                        .property(S3BucketSchema.REGION, S3_REGION)
                        .property(S3BucketSchema.BUCKET_NAME, S3_CONSUMER_BUCKET_NAME)
                        .property(S3BucketSchema.ACCESS_KEY_ID, S3_ACCESS_KEY_ID)
                        .property(S3BucketSchema.SECRET_ACCESS_KEY, SECRET_ACCESS_KEY)
                        .property(S3BucketSchema.ENDPOINT_OVERRIDE, consumerEndpointOverride)
                        .build()
                )
                .processId("test-process-id")
                .build();
    }


    private DataFlowStartMessage createMultipleFileFlowRequest() {
        return DataFlowStartMessage.Builder.newInstance()
                .id("test-process-multiple-file-id")
                .sourceDataAddress(DataAddress.Builder.newInstance()
                        .type(S3BucketSchema.TYPE)
                        .property(S3BucketSchema.KEY_PREFIX, PREFIX_FOR_MUTIPLE_FILES)
                        .property(S3BucketSchema.REGION, S3_REGION)
                        .property(S3BucketSchema.BUCKET_NAME, S3_PROVIDER_BUCKET_NAME)
                        .property(S3BucketSchema.ACCESS_KEY_ID, S3_ACCESS_KEY_ID)
                        .property(S3BucketSchema.SECRET_ACCESS_KEY, SECRET_ACCESS_KEY)
                        .property(S3BucketSchema.ENDPOINT_OVERRIDE, providerEndpointOverride)
                        .build()
                )
                .destinationDataAddress(DataAddress.Builder.newInstance()
                        .type(S3BucketSchema.TYPE)
                        .property(S3BucketSchema.REGION, S3_REGION)
                        .property(S3BucketSchema.BUCKET_NAME, S3_CONSUMER_BUCKET_NAME)
                        .property(S3BucketSchema.ACCESS_KEY_ID, S3_ACCESS_KEY_ID)
                        .property(S3BucketSchema.SECRET_ACCESS_KEY, SECRET_ACCESS_KEY)
                        .property(S3BucketSchema.ENDPOINT_OVERRIDE, consumerEndpointOverride)
                        .build()
                )
                .processId("test-process-multiple-file-id")
                .build();
    }

}
