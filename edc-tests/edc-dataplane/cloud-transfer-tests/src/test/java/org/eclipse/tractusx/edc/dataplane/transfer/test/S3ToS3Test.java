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
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.aws.s3.spi.S3BucketSchema;
import org.eclipse.edc.connector.controlplane.test.system.utils.LazySupplier;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.tractusx.edc.tests.aws.MinioExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.junit.jupiter.Testcontainers;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage.EDC_DATA_FLOW_START_MESSAGE_TYPE;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.PREFIX_FOR_MUTIPLE_FILES;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.TESTFILE_NAME;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestFunctions.createSparseFile;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestFunctions.listObjects;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

/**
 * This test is intended to verify transfers within the same cloud provider, i.e. S3-to-S3.
 * It spins up a fully-fledged dataplane and issues the DataFlowStartMessage via the data plane's Control API
 */
@Testcontainers
@EndToEndTest
public class S3ToS3Test {

    private static final LazySupplier<URI> CONTROL_API_URI = new LazySupplier<>(() -> URI.create("http://localhost:%s/control".formatted(getFreePort())));
    private static final LazySupplier<URI> START_DATAFLOW_URI = new LazySupplier<>(() -> URI.create("%s/v1/dataflows".formatted(CONTROL_API_URI.get())));

    @RegisterExtension
    protected static final RuntimeExtension DATAPLANE_RUNTIME = new RuntimePerClassExtension(
            new EmbeddedRuntime("AwsS3-Dataplane", ":edc-tests:runtime:dataplane-cloud")
                    .configurationProvider(() -> RuntimeConfig.S3.s3dataplaneConfig(CONTROL_API_URI))
    ).registerServiceMock(Monitor.class, spy(new ConsoleMonitor("AwsS3-Dataplane", ConsoleMonitor.Level.DEBUG, true)));

    @RegisterExtension
    private static final MinioExtension PROVIDER_CONTAINER = new MinioExtension();

    @RegisterExtension
    private static final MinioExtension CONSUMER_CONTAINER = new MinioExtension();

    private final S3Client providerClient = PROVIDER_CONTAINER.s3Client();
    private final S3Client consumerClient = CONSUMER_CONTAINER.s3Client();
    private final String providerEndpointOverride = PROVIDER_CONTAINER.getEndpointOverride();
    private final String consumerEndpointOverride = CONSUMER_CONTAINER.getEndpointOverride();

    @Test
    void transferMultipleFiles() {
        var sourceBucketName = UUID.randomUUID().toString();
        var sourceBucket = providerClient.createBucket(CreateBucketRequest.builder().bucket(sourceBucketName).build());
        assertThat(sourceBucket.sdkHttpResponse().isSuccessful()).isTrue();
        var putResponse = new AtomicBoolean(true);
        var filesNames = new ArrayDeque<String>();

        var fileNames = IntStream.rangeClosed(1, 2).mapToObj(i -> PREFIX_FOR_MUTIPLE_FILES + i + '_' + TESTFILE_NAME).toList();
        fileNames.forEach(filename -> putResponse.set(providerClient.putObject(PutObjectRequest.builder()
                        .bucket(sourceBucketName)
                        .key(filename)
                        .build(), TestUtils.getFileFromResourceName(TESTFILE_NAME).toPath())
                .sdkHttpResponse()
                .isSuccessful() && putResponse.get()));

        assertThat(putResponse.get()).isTrue();

        var destinationBucketName = UUID.randomUUID().toString();
        var destinationBucket = consumerClient.createBucket(CreateBucketRequest.builder().bucket(destinationBucketName).build());
        assertThat(destinationBucket.sdkHttpResponse().isSuccessful()).isTrue();
        var additionalSourceAddressProperties = List.of(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.OBJECT_PREFIX, PREFIX_FOR_MUTIPLE_FILES));

        given().when()
                .baseUri(START_DATAFLOW_URI.get().toString())
                .contentType(ContentType.JSON)
                .body(createDataFlowStartMessage(sourceBucketName, destinationBucketName, additionalSourceAddressProperties))
                .post()
                .then()
                .statusCode(200);

        await().pollInterval(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(60))
                .untilAsserted(() ->
                        assertThat(listObjects(consumerClient, destinationBucketName))
                                .isNotEmpty()
                                .containsAll(filesNames));
    }

    @Test
    void transferFile_success() {
        var sourceBucketName = UUID.randomUUID().toString();
        var b1 = providerClient.createBucket(CreateBucketRequest.builder().bucket(sourceBucketName).build());
        assertThat(b1.sdkHttpResponse().isSuccessful()).isTrue();
        // upload test file in provider
        var putResponse = providerClient.putObject(PutObjectRequest.builder().bucket(sourceBucketName).key(TESTFILE_NAME).build(), TestUtils.getFileFromResourceName(TESTFILE_NAME).toPath());
        assertThat(putResponse.sdkHttpResponse().isSuccessful()).isTrue();

        // create bucket in consumer
        var destinationBucketName = UUID.randomUUID().toString();
        var b2 = consumerClient.createBucket(CreateBucketRequest.builder().bucket(destinationBucketName).build());
        assertThat(b2.sdkHttpResponse().isSuccessful()).isTrue();

        var additionalSourceAddressProperties = List.of(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.OBJECT_NAME, TESTFILE_NAME));

        given().when()
                .baseUri(START_DATAFLOW_URI.get().toString())
                .contentType(ContentType.JSON)
                .body(createDataFlowStartMessage(sourceBucketName, destinationBucketName, additionalSourceAddressProperties))
                .post()
                .then()
                .statusCode(200);


        await().pollInterval(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(60))
                .untilAsserted(() -> assertThat(listObjects(consumerClient, destinationBucketName))
                        .isNotEmpty()
                        .contains(TESTFILE_NAME));
    }

    @Test
    void transferFile_targetContainerNotExist_shouldFail() {
        // create bucket in provider
        var sourceBucketName = UUID.randomUUID().toString();
        var b1 = providerClient.createBucket(CreateBucketRequest.builder().bucket(sourceBucketName).build());
        assertThat(b1.sdkHttpResponse().isSuccessful()).isTrue();
        // upload test file in provider
        var putResponse = providerClient.putObject(PutObjectRequest.builder().bucket(sourceBucketName).key(TESTFILE_NAME).build(), TestUtils.getFileFromResourceName(TESTFILE_NAME).toPath());
        assertThat(putResponse.sdkHttpResponse().isSuccessful()).isTrue();

        // do not create bucket in consumer -> will fail!
        var additionalSourceAddressProperties = List.of(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.OBJECT_NAME, TESTFILE_NAME));

        given().when()
                .baseUri(START_DATAFLOW_URI.get().toString())
                .contentType(ContentType.JSON)
                .body(createDataFlowStartMessage(sourceBucketName, "not-existent-bucket", additionalSourceAddressProperties))
                .post()
                .then()
                .statusCode(200);

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
        var sourceBucketName = UUID.randomUUID().toString();
        var b1 = providerClient.createBucket(CreateBucketRequest.builder().bucket(sourceBucketName).build());
        assertThat(b1.sdkHttpResponse().isSuccessful()).isTrue();
        // upload test file in provider
        uploadLargeFile(file, sourceBucketName, TESTFILE_NAME).thenAccept(completedUpload -> {
            // create bucket in consumer
            var destinationBucketName = UUID.randomUUID().toString();
            var b2 = consumerClient.createBucket(CreateBucketRequest.builder().bucket(destinationBucketName).build());
            assertThat(b2.sdkHttpResponse().isSuccessful()).isTrue();

            var additionalSourceAddressProperties = List.of(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.OBJECT_NAME, TESTFILE_NAME));

            given().when()
                    .baseUri(START_DATAFLOW_URI.get().toString())
                    .contentType(ContentType.JSON)
                    .body(createDataFlowStartMessage(sourceBucketName, destinationBucketName, additionalSourceAddressProperties))
                    .post()
                    .then()
                    .statusCode(200);


            await().pollInterval(Duration.ofSeconds(2))
                    .atMost(Duration.ofSeconds(60))
                    .untilAsserted(() -> assertThat(listObjects(consumerClient, destinationBucketName))
                            .isNotEmpty()
                            .contains(TESTFILE_NAME));
        });

    }

    private CompletableFuture<CompletedUpload> uploadLargeFile(File file, String bucketName, String fileName) {
        var transferManager = S3TransferManager.builder().s3Client(PROVIDER_CONTAINER.s3AsyncClient()).build();

        return transferManager.upload(UploadRequest.builder()
                .putObjectRequest(PutObjectRequest.builder().key(fileName).bucket(bucketName).build())
                .requestBody(AsyncRequestBody.fromFile(file))
                .build())
                .completionFuture();
    }

    private JsonObject createDataFlowStartMessage(String sourceBucketName, String destinationBucketName, List<JsonObjectBuilder> additionalSourceAddressProperties) {
        return Json.createObjectBuilder()
                .add("@context", Json.createObjectBuilder().add("@vocab", EDC_NAMESPACE).add("dspace", "https://w3id.org/dspace/v0.8/"))
                .add("@type", EDC_DATA_FLOW_START_MESSAGE_TYPE)
                .add("@id", UUID.randomUUID().toString())
                .add("processId", UUID.randomUUID().toString())
                .add("sourceDataAddress", Json.createObjectBuilder()
                        .add("dspace:endpointType", S3BucketSchema.TYPE)
                        .add("dspace:endpointProperties", Json.createArrayBuilder(additionalSourceAddressProperties)
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.REGION, PROVIDER_CONTAINER.getS3region()))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.BUCKET_NAME, sourceBucketName))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.ACCESS_KEY_ID, PROVIDER_CONTAINER.getCredentials().accessKeyId()))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.SECRET_ACCESS_KEY, PROVIDER_CONTAINER.getCredentials().secretAccessKey()))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.ENDPOINT_OVERRIDE, providerEndpointOverride))
                        )
                )
                .add("destinationDataAddress", Json.createObjectBuilder()
                        .add("dspace:endpointType", S3BucketSchema.TYPE)
                        .add("dspace:endpointProperties", Json.createArrayBuilder()
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.OBJECT_NAME, TESTFILE_NAME))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.REGION, PROVIDER_CONTAINER.getS3region()))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.BUCKET_NAME, destinationBucketName))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.ACCESS_KEY_ID, CONSUMER_CONTAINER.getCredentials().accessKeyId()))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.SECRET_ACCESS_KEY, CONSUMER_CONTAINER.getCredentials().secretAccessKey()))
                                .add(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.ENDPOINT_OVERRIDE, consumerEndpointOverride))
                        )
                )
                .add("flowType", "PUSH")
                .add("transferTypeDestination", "AmazonS3-PUSH")
                .build();
    }

    private JsonObjectBuilder dspaceProperty(String name, String value) {
        return Json.createObjectBuilder()
                .add("dspace:name", name)
                .add("dspace:value", value);
    }

}
