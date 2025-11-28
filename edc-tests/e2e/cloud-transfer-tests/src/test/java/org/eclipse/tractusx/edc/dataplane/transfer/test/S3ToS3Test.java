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
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.junit.utils.LazySupplier;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.tractusx.edc.tests.aws.MinioExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedUpload;
import software.amazon.awssdk.transfer.s3.model.UploadRequest;

import java.io.File;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.COMPLETED;
import static org.eclipse.edc.connector.dataplane.spi.DataFlowStates.FAILED;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFileFromResourceName;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage.EDC_DATA_FLOW_START_MESSAGE_TYPE;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.PREFIX_FOR_MUTIPLE_FILES;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestConstants.TESTFILE_NAME;
import static org.eclipse.tractusx.edc.dataplane.transfer.test.TestFunctions.createSparseFile;
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
                    .configurationProvider(() -> ConfigFactory.fromMap(Map.of(
                            "edc.dataplane.aws.sink.chunk.size.mb", "50"
                    )))
    );

    @RegisterExtension
    private static final MinioExtension PROVIDER_CONTAINER = new MinioExtension();

    @RegisterExtension
    private static final MinioExtension CONSUMER_CONTAINER = new MinioExtension();

    private final String providerEndpointOverride = PROVIDER_CONTAINER.getEndpointOverride();
    private final String consumerEndpointOverride = CONSUMER_CONTAINER.getEndpointOverride();

    @Test
    void transferMultipleFiles() {
        var sourceBucketName = PROVIDER_CONTAINER.createBucket();
        var filesNames = new ArrayDeque<String>();

        var fileNames = IntStream.rangeClosed(1, 2).mapToObj(i -> PREFIX_FOR_MUTIPLE_FILES + i + '_' + TESTFILE_NAME).toList();
        fileNames.forEach(filename -> PROVIDER_CONTAINER
                .uploadObjectOnBucket(sourceBucketName, filename, getFileFromResourceName(TESTFILE_NAME).toPath()));

        var destinationBucketName = CONSUMER_CONTAINER.createBucket();
        var additionalSourceAddressProperties = List.of(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.OBJECT_PREFIX, PREFIX_FOR_MUTIPLE_FILES));

        given().when()
                .baseUri(START_DATAFLOW_URI.get().toString())
                .contentType(ContentType.JSON)
                .body(createDataFlowStartMessage(sourceBucketName, destinationBucketName, additionalSourceAddressProperties, UUID.randomUUID().toString()))
                .post()
                .then()
                .statusCode(200);

        await().pollInterval(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(60))
                .untilAsserted(() -> assertThat(CONSUMER_CONTAINER.listObjects(destinationBucketName))
                        .isNotEmpty().containsAll(filesNames));
    }

    @Test
    void transferFile_success() {
        var sourceBucketName = PROVIDER_CONTAINER.createBucket();
        PROVIDER_CONTAINER.uploadObjectOnBucket(sourceBucketName, TESTFILE_NAME, getFileFromResourceName(TESTFILE_NAME).toPath());

        var destinationBucketName = CONSUMER_CONTAINER.createBucket();

        var additionalSourceAddressProperties = List.of(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.OBJECT_NAME, TESTFILE_NAME));

        given().when()
                .baseUri(START_DATAFLOW_URI.get().toString())
                .contentType(ContentType.JSON)
                .body(createDataFlowStartMessage(sourceBucketName, destinationBucketName, additionalSourceAddressProperties, UUID.randomUUID().toString()))
                .post()
                .then()
                .statusCode(200);

        await().pollInterval(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(60))
                .untilAsserted(() -> assertThat(CONSUMER_CONTAINER.listObjects(destinationBucketName))
                        .isNotEmpty().contains(TESTFILE_NAME));
    }

    @Test
    void shouldFail_whenDestinationBucketDoesNotExist() {
        var sourceBucketName = PROVIDER_CONTAINER.createBucket();
        PROVIDER_CONTAINER.uploadObjectOnBucket(sourceBucketName, TESTFILE_NAME, getFileFromResourceName(TESTFILE_NAME).toPath());

        var additionalSourceAddressProperties = List.of(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.OBJECT_NAME, TESTFILE_NAME));

        var processId = UUID.randomUUID().toString();
        given().when()
                .baseUri(START_DATAFLOW_URI.get().toString())
                .contentType(ContentType.JSON)
                .body(createDataFlowStartMessage(sourceBucketName, "not-existent-bucket", additionalSourceAddressProperties, processId))
                .post()
                .then()
                .statusCode(200);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var dataFlow = DATAPLANE_RUNTIME.getService(DataPlaneStore.class).findById(processId);
            assertThat(dataFlow.stateAsString()).isEqualTo(FAILED.name());
            assertThat(dataFlow.getErrorDetail()).contains("The specified bucket does not exist");
        });
    }

    @Test
    void shouldTransferOneGbFile() {
        var oneGigabyte = Math.pow(2, 30);
        var file = createSparseFile((long) oneGigabyte);

        var sourceBucketName = PROVIDER_CONTAINER.createBucket();
        var destinationBucketName = CONSUMER_CONTAINER.createBucket();

        var processId = UUID.randomUUID().toString();

        var future = uploadLargeFile(file, sourceBucketName, TESTFILE_NAME).thenAccept(completedUpload -> {

            var additionalSourceAddressProperties = List.of(dspaceProperty(EDC_NAMESPACE + S3BucketSchema.OBJECT_NAME, TESTFILE_NAME));

            given().when()
                    .baseUri(START_DATAFLOW_URI.get().toString())
                    .contentType(ContentType.JSON)
                    .body(createDataFlowStartMessage(sourceBucketName, destinationBucketName, additionalSourceAddressProperties, processId))
                    .post()
                    .then()
                    .statusCode(200)
                    .log().ifValidationFails();
        });

        assertThat(future).succeedsWithin(Duration.ofSeconds(10));
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            var dataFlow = DATAPLANE_RUNTIME.getService(DataPlaneStore.class).findById(processId);
            assertThat(dataFlow.stateAsString()).isEqualTo(COMPLETED.name());
            assertThat(dataFlow.getErrorDetail()).isNull();

            assertThat(CONSUMER_CONTAINER.listObjects(destinationBucketName))
                    .isNotEmpty().contains(TESTFILE_NAME);
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

    private JsonObject createDataFlowStartMessage(String sourceBucketName, String destinationBucketName, List<JsonObjectBuilder> additionalSourceAddressProperties, String processId) {
        return Json.createObjectBuilder()
                .add("@context", Json.createObjectBuilder().add("@vocab", EDC_NAMESPACE).add("dspace", "https://w3id.org/dspace/v0.8/"))
                .add("@type", EDC_DATA_FLOW_START_MESSAGE_TYPE)
                .add("@id", UUID.randomUUID().toString())
                .add("processId", processId)
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
