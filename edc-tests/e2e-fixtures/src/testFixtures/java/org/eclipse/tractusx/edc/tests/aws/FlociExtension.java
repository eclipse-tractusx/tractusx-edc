/*
 * Copyright (c) 2025 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 * Copyright (c) 2026 Cofinity-X GmbH
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
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.IamAsyncClient;
import software.amazon.awssdk.services.iam.model.CreateUserRequest;
import software.amazon.awssdk.services.iam.model.EntityAlreadyExistsException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;

public class FlociExtension implements BeforeAllCallback, AfterAllCallback {

    private static final DockerImageName FLOCI_IMAGE = DockerImageName.parse("hectorvent/floci:latest");
    private static final String S3_REGION = Region.US_WEST_2.id();
    private static final int EDGE_PORT = 4566;
    private static final String SYSTEM_PROPERTY_AWS_ACCESS_KEY_ID = "aws.accessKeyId";
    private static final String SYSTEM_PROPERTY_AWS_SECRET_ACCESS_KEY = "aws.secretAccessKey";
    private static final String SYSTEM_PROPERTY_AWS_REGION = "aws.region";
    private static final String STORAGE_PATH = "/app/data";
    private static final String ROOT_USER_NAME = "root";

    private final AwsCredentials credentials = AwsBasicCredentials.create("test-access-key", UUID.randomUUID().toString());
    @SuppressWarnings("resource")
    private final GenericContainer<?> flociContainer = new GenericContainer<>(FLOCI_IMAGE)
            .withExposedPorts(EDGE_PORT)
            .withEnv("FLOCI_STORAGE_MODE", "hybrid")
            .withEnv("FLOCI_STORAGE_PERSISTENT_PATH", STORAGE_PATH)
            .withTmpFs(Map.of(STORAGE_PATH, "rw"))
            .withLogConsumer(frame -> System.out.print(frame.getUtf8String()));
    private final LazySupplier<AwsClientProvider> clientProvider = new LazySupplier<>(() ->
            new AwsClientProviderImpl(getConfiguration()));

    @Override
    public void beforeAll(@NotNull ExtensionContext context) {
        System.setProperty(SYSTEM_PROPERTY_AWS_ACCESS_KEY_ID, credentials.accessKeyId());
        System.setProperty(SYSTEM_PROPERTY_AWS_SECRET_ACCESS_KEY, credentials.secretAccessKey());
        System.setProperty(SYSTEM_PROPERTY_AWS_REGION, S3_REGION);
        flociContainer.start();
        initializeRootUser();
    }

    @Override
    public void afterAll(@NotNull ExtensionContext context) {
        flociContainer.stop();
    }

    public AwsCredentials getCredentials() {
        return credentials;
    }

    public String getEndpointOverride() {
        return endpointUri().toString();
    }

    public S3Client s3Client() {
        return clientProvider.get().s3Client(S3ClientRequest.from(S3_REGION, getEndpointOverride()));
    }

    public String getS3region() {
        return S3_REGION;
    }

    @SuppressWarnings("resource")
    public String createBucket() {
        var bucketName = UUID.randomUUID().toString();
        var response = s3Client().createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
        assertThat(response.sdkHttpResponse().isSuccessful()).isTrue();
        return bucketName;
    }

    @SuppressWarnings("resource")
    public void uploadObjectOnBucket(String bucketName, String key, Path filePath) {
        var response = s3Client().putObject(PutObjectRequest.builder().bucket(bucketName).key(key).build(), filePath);
        assertThat(response.sdkHttpResponse().isSuccessful()).isTrue();
    }

    @SuppressWarnings("resource")
    public List<String> listObjects(String bucketName) {
        return s3Client().listObjects(ListObjectsRequest.builder().bucket(bucketName).build())
                .contents().stream().map(S3Object::key).toList();
    }

    private AwsClientProviderConfiguration getConfiguration() {
        return AwsClientProviderConfiguration.Builder.newInstance()
                .endpointOverride(endpointUri())
                .credentialsProvider(this::getCredentials)
                .build();
    }

    private void initializeRootUser() {
        try (var iamClient = IamAsyncClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.AWS_GLOBAL)
                .endpointOverride(endpointUri())
                .build()) {
            try {
                iamClient.createUser(CreateUserRequest.builder().userName(ROOT_USER_NAME).build()).join();
            } catch (CompletionException exception) {
                if (!(exception.getCause() instanceof EntityAlreadyExistsException)) {
                    throw new RuntimeException("Failed to initialize Floci IAM root user", exception);
                }
            }
        }
    }

    private URI endpointUri() {
        return URI.create("http://localhost:%s/".formatted(flociContainer.getMappedPort(EDGE_PORT)));
    }
}
