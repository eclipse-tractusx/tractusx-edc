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
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.tractusx.edc.tests.aws.MinioExtension;
import org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase;
import org.eclipse.tractusx.edc.tests.participant.TransferParticipant;
import org.eclipse.tractusx.edc.tests.runtimes.PostgresExtension;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;

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
import static org.eclipse.tractusx.edc.tests.runtimes.Runtimes.pgRuntime;

/**
 * This test is intended to verify transfers within the same cloud provider, i.e. S3-to-S3.
 * It spins up a fully-fledged dataplane and issues the DataFlowStartMessage via the data plane's Control API
 */
@EndToEndTest
public class S3ToS3EndToEndTest {

    private static final String TESTFILE_NAME = "hello.txt";

    private static final TransferParticipant CONSUMER = TransferParticipant.Builder.newInstance()
            .name(CONSUMER_NAME)
            .id(CONSUMER_BPN)
            .build();
    private static final TransferParticipant PROVIDER = TransferParticipant.Builder.newInstance()
            .name(PROVIDER_NAME)
            .id(PROVIDER_BPN)
            .build();

    @RegisterExtension
    @Order(0)
    private static final PostgresExtension POSTGRES = new PostgresExtension(PROVIDER.getName(), CONSUMER.getName());

    @RegisterExtension
    private static final RuntimeExtension PROVIDER_RUNTIME = pgRuntime(PROVIDER, POSTGRES, PROVIDER::getConfig);
    @RegisterExtension
    private static final RuntimeExtension CONSUMER_RUNTIME = pgRuntime(CONSUMER, POSTGRES, CONSUMER::getConfig);

    @RegisterExtension
    private static final MinioExtension PROVIDER_CONTAINER = new MinioExtension();
    @RegisterExtension
    private static final MinioExtension CONSUMER_CONTAINER = new MinioExtension();

    @Test
    void transferFile_success() {
        var assetId = "s3-test-asset";

        var sourceBucketName = PROVIDER_CONTAINER.createBucket();
        PROVIDER_CONTAINER.uploadObjectOnBucket(sourceBucketName, TESTFILE_NAME, TestUtils.getFileFromResourceName(TESTFILE_NAME).toPath());

        var destinationBucketName = CONSUMER_CONTAINER.createBucket();

        Map<String, Object> dataAddress = Map.of(
                "name", "transfer-test",
                "@type", "DataAddress",
                "type", "AmazonS3",
                "objectName", TESTFILE_NAME,
                "region", PROVIDER_CONTAINER.getS3region(),
                "bucketName", sourceBucketName,
                "accessKeyId", PROVIDER_CONTAINER.getCredentials().accessKeyId(),
                "secretAccessKey", PROVIDER_CONTAINER.getCredentials().secretAccessKey(),
                "endpointOverride", PROVIDER_CONTAINER.getEndpointOverride()
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
                        .add(EDC_NAMESPACE + "region", CONSUMER_CONTAINER.getS3region())
                        .add(EDC_NAMESPACE + "bucketName", destinationBucketName)
                        .add(EDC_NAMESPACE + "endpointOverride", CONSUMER_CONTAINER.getEndpointOverride())
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
            assertThat(CONSUMER_CONTAINER.listObjects(destinationBucketName)).isNotEmpty();
        });
    }

    public TractusxParticipantBase provider() {
        return PROVIDER;
    }

    public TractusxParticipantBase consumer() {
        return CONSUMER;
    }

}
