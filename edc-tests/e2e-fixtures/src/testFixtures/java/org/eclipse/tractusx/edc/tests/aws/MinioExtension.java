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

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.GenericContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;

import java.util.UUID;

public class MinioExtension implements BeforeAllCallback, AfterAllCallback {

    private final MinioContainer minioContainer = new MinioContainer();

    @Override
    public void beforeAll(ExtensionContext context) {
        minioContainer.start();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        minioContainer.stop();
    }

    public int getPort() {
        return minioContainer.getFirstMappedPort();
    }

    public AwsCredentials getCredentials() {
        return minioContainer.getCredentials();
    }

    private static class MinioContainer extends GenericContainer<MinioContainer> {

        private final String accessKeyId = "test-access-key";
        private final String secretAccessKey = UUID.randomUUID().toString();

        public MinioContainer() {
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
