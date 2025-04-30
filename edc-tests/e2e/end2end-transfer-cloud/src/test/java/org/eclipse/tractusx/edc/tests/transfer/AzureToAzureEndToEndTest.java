/*
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft
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

package org.eclipse.tractusx.edc.tests.transfer;

import com.azure.core.util.BinaryData;
import org.apache.commons.codec.binary.Base64;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.tractusx.edc.tests.azure.AzureBlobClient;
import org.eclipse.tractusx.edc.tests.azure.AzuriteExtension;
import org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase;
import org.eclipse.tractusx.edc.tests.participant.TransferParticipant;
import org.eclipse.tractusx.edc.tests.runtimes.PostgresExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;
import java.util.UUID;

import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.CONSUMER_NAME;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_NAME;
import static org.eclipse.tractusx.edc.tests.helpers.PolicyHelperFunctions.bnpPolicy;
import static org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase.ASYNC_TIMEOUT;
import static org.eclipse.tractusx.edc.tests.runtimes.Runtimes.pgRuntime;

/**
 * This test runs through a contract negotiation and transfer process phase, then transfers files from an Az Blob container
 * to another Az blob container
 */
@EndToEndTest
public class AzureToAzureEndToEndTest {

    public static final String PROVIDER_KEY_ALIAS = "provider-key-alias";
    private static final TransferParticipant CONSUMER = TransferParticipant.Builder.newInstance()
            .name(CONSUMER_NAME)
            .id(CONSUMER_BPN)
            .build();
    private static final TransferParticipant PROVIDER = TransferParticipant.Builder.newInstance()
            .name(PROVIDER_NAME)
            .id(PROVIDER_BPN)
            .build();
    private static final int AZURITE_HOST_PORT = getFreePort();

    @RegisterExtension
    @Order(0)
    private static final PostgresExtension POSTGRES = new PostgresExtension(PROVIDER.getName(), CONSUMER.getName());

    @RegisterExtension
    private static final RuntimeExtension PROVIDER_RUNTIME = pgRuntime(PROVIDER, POSTGRES,
            () -> PROVIDER.getConfig().merge(additionalAzureConfig()));

    @RegisterExtension
    private static final RuntimeExtension CONSUMER_RUNTIME = pgRuntime(CONSUMER, POSTGRES,
            () -> CONSUMER.getConfig().merge(additionalAzureConfig()));

    private static final AzuriteExtension.Account PROVIDER_AZURITE_ACCOUNT = new AzuriteExtension.Account("provider", Base64.encodeBase64String("provider-key".getBytes()));
    private static final AzuriteExtension.Account CONSUMER_AZURITE_ACCOUNT = new AzuriteExtension.Account("consumer", Base64.encodeBase64String("consumer-key".getBytes()));

    @RegisterExtension
    private static final AzuriteExtension AZURITE_CONTAINER = new AzuriteExtension(AZURITE_HOST_PORT, PROVIDER_AZURITE_ACCOUNT, CONSUMER_AZURITE_ACCOUNT);

    private static final String TESTFILE_NAME = "hello.txt";

    private AzureBlobClient providerBlobHelper;
    private AzureBlobClient consumerBlobHelper;

    private static Config additionalAzureConfig() {
        return ConfigFactory.fromMap(Map.of("edc.blobstore.endpoint.template", "http://127.0.0.1:" + AZURITE_HOST_PORT + "/%s"));
    }

    public TractusxParticipantBase provider() {
        return PROVIDER;
    }

    public TractusxParticipantBase consumer() {
        return CONSUMER;
    }

    @BeforeEach
    void setup() {
        PROVIDER_RUNTIME.getService(Vault.class)
                .storeSecret(PROVIDER_KEY_ALIAS, PROVIDER_AZURITE_ACCOUNT.key());

        CONSUMER_RUNTIME.getService(Vault.class)
                .storeSecret("%s-key1".formatted(CONSUMER_AZURITE_ACCOUNT.name()), CONSUMER_AZURITE_ACCOUNT.key());

        providerBlobHelper = AZURITE_CONTAINER.getClientFor(PROVIDER_AZURITE_ACCOUNT);
        consumerBlobHelper = AZURITE_CONTAINER.getClientFor(CONSUMER_AZURITE_ACCOUNT);
    }

    @Test
    void azureBlobPush_withDestFolder() {
        var assetId = "felix-blob-test-asset";

        var sourceContainerName = UUID.randomUUID().toString();
        Map<String, Object> dataAddress = Map.of(
                "name", "transfer-test",
                "@type", "DataAddress",
                "type", "AzureStorage",
                "container", sourceContainerName,
                "account", PROVIDER_AZURITE_ACCOUNT.name(),
                "blobPrefix", "folder/",
                "keyName", PROVIDER_KEY_ALIAS
        );

        // upload file to provider's blob store
        var sourceContainer = providerBlobHelper.createContainer(sourceContainerName);
        var fileData = BinaryData.fromString(TestUtils.getResourceFileContentAsString(TESTFILE_NAME));
        providerBlobHelper.uploadBlob(sourceContainer, fileData, "folder/" + TESTFILE_NAME);
        var destinationContainerName = UUID.randomUUID().toString();
        consumerBlobHelper.createContainer(destinationContainerName);

        // create objects in EDC
        provider().createAsset(assetId, Map.of(), dataAddress);
        var policyId = provider().createPolicyDefinition(bnpPolicy(consumer().getBpn()));
        provider().createContractDefinition(assetId, "def-1", policyId, policyId);

        var destfolder = "destfolder";
        var destination = createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "DataAddress")
                .add(EDC_NAMESPACE + "type", "AzureStorage")
                .add(EDC_NAMESPACE + "properties", createObjectBuilder()
                        .add(EDC_NAMESPACE + "type", "AzureStorage")
                        .add(EDC_NAMESPACE + "account", CONSUMER_AZURITE_ACCOUNT.name())
                        .add(EDC_NAMESPACE + "container", destinationContainerName)
                        .add(EDC_NAMESPACE + "folderName", destfolder)
                        .build())
                .build();

        // perform contract negotiation and transfer process
        var transferProcessId = consumer()
                .requestAssetFrom(assetId, provider())
                .withTransferType("AzureStorage-PUSH")
                .withDestination(destination)
                .execute();

        await().atMost(ASYNC_TIMEOUT).untilAsserted(() -> {
            var state = consumer().getTransferProcessState(transferProcessId);
            assertThat(state).isEqualTo(COMPLETED.name());
            assertThat(consumerBlobHelper.listBlobs(destinationContainerName))
                    .contains("%s/folder/%s".formatted(destfolder, TESTFILE_NAME));
        });
    }

    @Test
    void azureBlobPush_withoutDestFolder() {
        var assetId = "felix-blob-test-asset";

        var sourceContainerName = UUID.randomUUID().toString();
        Map<String, Object> dataAddress = Map.of(
                "name", "transfer-test",
                "@type", "DataAddress",
                "type", "AzureStorage",
                "container", sourceContainerName,
                "account", PROVIDER_AZURITE_ACCOUNT.name(),
                "blobPrefix", "folder/",
                "keyName", PROVIDER_KEY_ALIAS
        );

        // upload file to provider's blob store
        var sourceContainer = providerBlobHelper.createContainer(sourceContainerName);
        var fileData = BinaryData.fromString(TestUtils.getResourceFileContentAsString(TESTFILE_NAME));
        providerBlobHelper.uploadBlob(sourceContainer, fileData, "folder/" + TESTFILE_NAME);
        var destinationContainerName = UUID.randomUUID().toString();
        consumerBlobHelper.createContainer(destinationContainerName);

        // create objects in EDC
        provider().createAsset(assetId, Map.of(), dataAddress);
        var policyId = provider().createPolicyDefinition(bnpPolicy(consumer().getBpn()));
        provider().createContractDefinition(assetId, "def-1", policyId, policyId);

        var destination = createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "DataAddress")
                .add(EDC_NAMESPACE + "type", "AzureStorage")
                .add(EDC_NAMESPACE + "properties", createObjectBuilder()
                        .add(EDC_NAMESPACE + "type", "AzureStorage")
                        .add(EDC_NAMESPACE + "account", CONSUMER_AZURITE_ACCOUNT.name())
                        .add(EDC_NAMESPACE + "container", destinationContainerName)
                        // .add(EDC_NAMESPACE + "folderName", destfolder) <-- no dest folder
                        .build())
                .build();

        // perform contract negotiation and transfer process
        var transferProcessId = consumer()
                .requestAssetFrom(assetId, provider())
                .withTransferType("AzureStorage-PUSH")
                .withDestination(destination)
                .execute();

        await().atMost(ASYNC_TIMEOUT).untilAsserted(() -> {
            var state = consumer().getTransferProcessState(transferProcessId);
            assertThat(state).isEqualTo(COMPLETED.name());
            assertThat(consumerBlobHelper.listBlobs(destinationContainerName))
                    .contains("folder/%s".formatted(TESTFILE_NAME));
        });
    }

    @Test
    void azureBlobPush_containerNotExist() {
        var assetId = "blob-test-asset";

        var sourceContainerName = UUID.randomUUID().toString();
        Map<String, Object> dataAddress = Map.of(
                "name", "transfer-test",
                "@type", "DataAddress",
                "type", "AzureStorage",
                "container", sourceContainerName,
                "account", PROVIDER_AZURITE_ACCOUNT.name(),
                "blobPrefix", "folder/",
                "keyName", PROVIDER_KEY_ALIAS
        );

        // upload file to provider's blob store
        var sourceContainer = providerBlobHelper.createContainer(sourceContainerName);
        var fileData = BinaryData.fromString(TestUtils.getResourceFileContentAsString(TESTFILE_NAME));
        providerBlobHelper.uploadBlob(sourceContainer, fileData, "folder/" + TESTFILE_NAME);

        // create objects in EDC
        provider().createAsset(assetId, Map.of(), dataAddress);
        var policyId = provider().createPolicyDefinition(bnpPolicy(consumer().getBpn()));
        provider().createContractDefinition(assetId, "def-1", policyId, policyId);

        var destinationContainerName = UUID.randomUUID().toString();
        var destination = createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "DataAddress")
                .add(EDC_NAMESPACE + "type", "AzureStorage")
                .add(EDC_NAMESPACE + "properties", createObjectBuilder()
                        .add(EDC_NAMESPACE + "type", "AzureStorage")
                        .add(EDC_NAMESPACE + "account", CONSUMER_AZURITE_ACCOUNT.name())
                        .add(EDC_NAMESPACE + "container", destinationContainerName)
                        .build())
                .build();

        // perform contract negotiation and transfer process
        var transferProcessId = consumer()
                .requestAssetFrom(assetId, provider())
                .withTransferType("AzureStorage-PUSH")
                .withDestination(destination)
                .execute();

        await().atMost(ASYNC_TIMEOUT).untilAsserted(() -> {
            var state = consumer().getTransferProcessState(transferProcessId);
            assertThat(state).isEqualTo(COMPLETED.name());
            assertThat(consumerBlobHelper.listBlobs(destinationContainerName))
                    .contains("folder/%s".formatted(TESTFILE_NAME));
        });
    }

}
