/*
 * Copyright (c) 2024 Cofinity-X
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
import org.eclipse.edc.connector.controlplane.test.system.utils.LazySupplier;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.GenericContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;
import java.util.UUID;

public class MinioExtension implements BeforeAllCallback, AfterAllCallback {

    private static final String S3_REGION = Region.US_WEST_2.id();
    private final MinioContainer minioContainer = new MinioContainer();
    private final LazySupplier<AwsClientProvider> clientProvider = new LazySupplier<>(() ->
            new AwsClientProviderImpl(getConfiguration()));

    @Override
    public void beforeAll(ExtensionContext context) {
        minioContainer.start();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        minioContainer.stop();
    }

    public AwsCredentials getCredentials() {
        return minioContainer.getCredentials();
    }

    public String getEndpointOverride() {
        return "http://localhost:%s/".formatted(minioContainer.getFirstMappedPort());
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

    private AwsClientProviderConfiguration getConfiguration() {
        return AwsClientProviderConfiguration.Builder.newInstance()
                .endpointOverride(URI.create(getEndpointOverride()))
                .credentialsProvider(this::getCredentials)
                .build();
    }

    private static class MinioContainer extends GenericContainer<MinioContainer> {

        private final String accessKeyId = "test-access-key";
        private final String secretAccessKey = UUID.randomUUID().toString();

        MinioContainer() {
            super("bitnami/minio");
            addEnv("MINIO_ROOT_USER", accessKeyId);
            addEnv("MINIO_ROOT_PASSWORD", secretAccessKey);
            addExposedPort(9000);
        }

        public AwsBasicCredentials getCredentials() {
            return AwsBasicCredentials.create(accessKeyId, secretAccessKey);
        }
    }
}
