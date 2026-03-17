/*
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
 */

package org.eclipse.tractusx.edc.tests.aws;

import org.eclipse.edc.aws.s3.AwsClientProvider;
import org.eclipse.edc.aws.s3.AwsClientProviderConfiguration;
import org.eclipse.edc.aws.s3.AwsClientProviderImpl;
import org.eclipse.edc.aws.s3.S3ClientRequest;
import org.eclipse.edc.junit.utils.LazySupplier;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class LocalstackExtension implements BeforeAllCallback, AfterAllCallback {

    private static final String S3_REGION = Region.US_WEST_2.id();
    private static final String SYSTEM_PROPERTY_AWS_ACCESS_KEY_ID = "aws.accessKeyId";
    private static final String SYSTEM_PROPERTY_AWS_SECRET_ACCESS_KEY = "aws.secretAccessKey";

    private final String accessKeyId = "test-access-key";
    private final String secretAccessKey = UUID.randomUUID().toString();
    private final LazySupplier<AwsClientProvider> clientProvider = new LazySupplier<>(() ->
            new AwsClientProviderImpl(getConfiguration()));

    private final LocalStackContainer localStackContainer = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack")
    ).withServices(LocalStackContainer.Service.S3, LocalStackContainer.Service.IAM, LocalStackContainer.Service.STS)
            .withEnv("DEFAULT_REGION", S3_REGION)
            .withEnv("AWS_ACCESS_KEY_ID", accessKeyId)
            .withEnv("AWS_SECRET_ACCESS_KEY", secretAccessKey)
            .withExposedPorts(4566, 9000)
            .withLogConsumer(frame -> System.out.print(frame.getUtf8String()));

    @Override
    public void beforeAll(ExtensionContext context) {
        System.setProperty(SYSTEM_PROPERTY_AWS_ACCESS_KEY_ID, accessKeyId);
        System.setProperty(SYSTEM_PROPERTY_AWS_SECRET_ACCESS_KEY, secretAccessKey);
        localStackContainer.start();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        localStackContainer.stop();
    }

    public AwsCredentials getCredentials() {
        return AwsBasicCredentials.create(accessKeyId, secretAccessKey);
    }

    public String getEndpointOverride() {
        return "http://localhost:%s/".formatted(localStackContainer.getFirstMappedPort());
    }

    public S3Client s3Client() {
        return clientProvider.get().s3Client(S3ClientRequest.from(S3_REGION, getEndpointOverride()));
    }

    public S3AsyncClient s3AsyncClient() {
        return clientProvider.get().s3AsyncClient(S3ClientRequest.from(S3_REGION, getEndpointOverride()));
    }

    public String getS3region() {
        return S3_REGION;
    }

    public String createBucket() {
        var bucketName = UUID.randomUUID().toString();
        var response = s3Client().createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
        assertThat(response.sdkHttpResponse().isSuccessful()).isTrue();
        return bucketName;
    }

    public void uploadObjectOnBucket(String bucketName, String key, Path filePath) {
        var response = s3Client().putObject(PutObjectRequest.builder().bucket(bucketName).key(key).build(), filePath);
        assertThat(response.sdkHttpResponse().isSuccessful()).isTrue();
    }

    public List<String> listObjects(String bucketName) {
        return s3Client().listObjects(ListObjectsRequest.builder().bucket(bucketName).build())
                .contents().stream().map(S3Object::key).toList();
    }

    private AwsClientProviderConfiguration getConfiguration() {
        return AwsClientProviderConfiguration.Builder.newInstance()
                .endpointOverride(URI.create(getEndpointOverride()))
                .credentialsProvider(this::getCredentials)
                .build();
    }

}
