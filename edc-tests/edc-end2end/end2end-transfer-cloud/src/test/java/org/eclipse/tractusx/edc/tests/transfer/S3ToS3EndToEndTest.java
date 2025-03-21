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

package org.eclipse.tractusx.edc.tests.transfer;

import jakarta.json.Json;
import org.eclipse.edc.aws.s3.AwsClientProviderConfiguration;
import org.eclipse.edc.aws.s3.AwsClientProviderImpl;
import org.eclipse.edc.aws.s3.S3ClientRequest;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.tractusx.edc.tests.aws.MinioExtension;
import org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase;
import org.eclipse.tractusx.edc.tests.participant.TransferParticipant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_NAME;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_NAME;
import static org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions.bnpPolicy;
import static org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase.ASYNC_TIMEOUT;
import static org.eclipse.tractusx.edc.tests.runtimes.Runtimes.memoryRuntime;

/**
 * This test is intended to verify transfers within the same cloud provider, i.e. S3-to-S3.
 * It spins up a fully-fledged dataplane and issues the DataFlowStartMessage via the data plane's Control API
 */
@Testcontainers
@EndToEndTest
public class S3ToS3EndToEndTest {
    protected static final TransferParticipant CONSUMER = TransferParticipant.Builder.newInstance()
            .name(CONSUMER_NAME)
            .id(CONSUMER_BPN)
            .build();
    protected static final TransferParticipant PROVIDER = TransferParticipant.Builder.newInstance()
            .name(PROVIDER_NAME)
            .id(PROVIDER_BPN)
            .build();
    @RegisterExtension
    protected static final RuntimeExtension PROVIDER_RUNTIME = memoryRuntime(PROVIDER.getName(), PROVIDER.getBpn(), PROVIDER::getConfig);
    @RegisterExtension
    protected static final RuntimeExtension CONSUMER_RUNTIME = memoryRuntime(CONSUMER.getName(), CONSUMER.getBpn(), CONSUMER::getConfig);

    private static final String S3_REGION = Region.US_WEST_2.id();
    private static final String TESTFILE_NAME = "hello.txt";

    @RegisterExtension
    private static final MinioExtension PROVIDER_CONTAINER = new MinioExtension();
    @RegisterExtension
    private static final MinioExtension CONSUMER_CONTAINER = new MinioExtension();

    private S3Client providerClient;
    private S3Client consumerClient;
    private String providerEndpointOverride;
    private String consumerEndpointOverride;

    @BeforeEach
    void setup() {
        providerEndpointOverride = "http://localhost:%s/".formatted(PROVIDER_CONTAINER.getPort());
        var providerConfig = AwsClientProviderConfiguration.Builder.newInstance()
                .endpointOverride(URI.create(providerEndpointOverride))
                .credentialsProvider(PROVIDER_CONTAINER::getCredentials)
                .build();
        providerClient = new AwsClientProviderImpl(providerConfig).s3Client(S3ClientRequest.from(S3_REGION, providerEndpointOverride));

        consumerEndpointOverride = "http://localhost:%s".formatted(CONSUMER_CONTAINER.getPort());
        var consumerConfig = AwsClientProviderConfiguration.Builder.newInstance()
                .endpointOverride(URI.create(consumerEndpointOverride))
                .credentialsProvider(CONSUMER_CONTAINER::getCredentials)
                .build();
        consumerClient = new AwsClientProviderImpl(consumerConfig).s3Client(S3ClientRequest.from(S3_REGION, consumerEndpointOverride));
    }

    @Test
    void transferFile_success() {
        var assetId = "s3-test-asset";

        // create bucket in provider
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

        Map<String, Object> dataAddress = Map.of(
                "name", "transfer-test",
                "@type", "DataAddress",
                "type", "AmazonS3",
                "objectName", TESTFILE_NAME,
                "region", S3_REGION,
                "bucketName", sourceBucketName,
                "accessKeyId", PROVIDER_CONTAINER.getCredentials().accessKeyId(),
                "secretAccessKey", PROVIDER_CONTAINER.getCredentials().secretAccessKey(),
                "endpointOverride", providerEndpointOverride
        );

        // create objects in EDC
        provider().createAsset(assetId, Map.of(), dataAddress);
        var policyId = provider().createPolicyDefinition(bnpPolicy(consumer().getBpn()));
        provider().createContractDefinition(assetId, "def-1", policyId, policyId);


        var destination = Json.createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "DataAddress")
                .add(EDC_NAMESPACE + "type", "AmazonS3")
                .add(EDC_NAMESPACE + "properties", Json.createObjectBuilder()
                        .add(EDC_NAMESPACE + "type", "AmazonS3")
                        .add(EDC_NAMESPACE + "objectName", TESTFILE_NAME)
                        .add(EDC_NAMESPACE + "region", S3_REGION)
                        .add(EDC_NAMESPACE + "bucketName", destinationBucketName)
                        .add(EDC_NAMESPACE + "endpointOverride", consumerEndpointOverride)
                        .add(EDC_NAMESPACE + "accessKeyId", CONSUMER_CONTAINER.getCredentials().accessKeyId())
                        .add(EDC_NAMESPACE + "secretAccessKey", CONSUMER_CONTAINER.getCredentials().secretAccessKey())
                        .build()
                ).build();

        var transferProcessId = consumer()
                .requestAssetFrom(assetId, provider())
                .withTransferType("AmazonS3-PUSH")
                .withDestination(destination)
                .execute();

        await().atMost(ASYNC_TIMEOUT).untilAsserted(() -> {
            var state = consumer().getTransferProcessState(transferProcessId);
            assertThat(state).isEqualTo(COMPLETED.name());
            var rq = ListObjectsRequest.builder().bucket(destinationBucketName).build();
            assertThat(consumerClient.listObjects(rq).contents()).isNotEmpty();
        });
    }

    public TractusxParticipantBase provider() {
        return PROVIDER;
    }

    public TractusxParticipantBase consumer() {
        return CONSUMER;
    }

}
