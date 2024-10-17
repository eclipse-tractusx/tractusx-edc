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
import org.eclipse.tractusx.edc.tests.participant.TractusxParticipantBase;
import org.eclipse.tractusx.edc.tests.participant.TransferParticipant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.Map;

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
import static org.eclipse.tractusx.edc.tests.runtimes.Runtimes.memoryRuntime;

/**
 * This test runs through a contract negotiation and transfer process phase, then transfers files from an Az Blob container
 * to another Az blob container
 */
@Testcontainers
@EndToEndTest
public class AzureToAzureEndToEndTest {

    public static final String AZURITE_DOCKER_IMAGE = "mcr.microsoft.com/azure-storage/azurite";
    public static final String CONSUMER_CONTAINER_NAME = "consumer-container";
    public static final String PROVIDER_CONTAINER_NAME = "provider-container";
    public static final String PROVIDER_KEY_ALIAS = "provider-key-alias";
    protected static final TransferParticipant CONSUMER = TransferParticipant.Builder.newInstance()
            .name(CONSUMER_NAME)
            .id(CONSUMER_BPN)
            .build();
    protected static final TransferParticipant PROVIDER = TransferParticipant.Builder.newInstance()
            .name(PROVIDER_NAME)
            .id(PROVIDER_BPN)
            .build();
    private static final int AZURITE_HOST_PORT = getFreePort();
    @RegisterExtension
    protected static final RuntimeExtension PROVIDER_RUNTIME = memoryRuntime(PROVIDER.getName(), PROVIDER.getBpn(), with(PROVIDER.getConfiguration(), AZURITE_HOST_PORT));
    @RegisterExtension
    protected static final RuntimeExtension CONSUMER_RUNTIME = memoryRuntime(CONSUMER.getName(), CONSUMER.getBpn(), with(CONSUMER.getConfiguration(), AZURITE_HOST_PORT));
    private static final String TESTFILE_NAME = "hello.txt";

    private final AzuriteContainer.Account providerAzuriteAccount = new AzuriteContainer.Account("provider", Base64.encodeBase64String("provider-key".getBytes()));
    private final AzuriteContainer.Account consumerAzuriteAccount = new AzuriteContainer.Account("consumer", Base64.encodeBase64String("consumer-key".getBytes()));
    @Container
    private final AzuriteContainer azuriteContainer = new AzuriteContainer(AZURITE_HOST_PORT, providerAzuriteAccount, consumerAzuriteAccount);
    private AzureBlobHelper providerBlobHelper;
    private AzureBlobHelper consumerBlobHelper;

    private static Map<String, String> with(Map<String, String> configuration, int port) {
        configuration.putAll(new HashMap<>() {
            {
                put("edc.blobstore.endpoint.template", "http://127.0.0.1:" + port + "/%s"); // set the Azure Blob endpoint template
            }
        });
        return configuration;
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
                .storeSecret(PROVIDER_KEY_ALIAS, providerAzuriteAccount.key());

        CONSUMER_RUNTIME.getService(Vault.class)
                .storeSecret("%s-key1".formatted(consumerAzuriteAccount.name()), consumerAzuriteAccount.key());

        providerBlobHelper = azuriteContainer.getHelper(providerAzuriteAccount);
        consumerBlobHelper = azuriteContainer.getHelper(consumerAzuriteAccount);
    }

    @Test
    void azureBlobPush_withDestFolder() {
        var assetId = "felix-blob-test-asset";

        Map<String, Object> dataAddress = Map.of(
                "name", "transfer-test",
                "@type", "DataAddress",
                "type", "AzureStorage",
                "container", PROVIDER_CONTAINER_NAME,
                "account", providerAzuriteAccount.name(),
                "blobPrefix", "folder/",
                "keyName", PROVIDER_KEY_ALIAS
        );

        // upload file to provider's blob store
        var sourceContainer = providerBlobHelper.createContainer(PROVIDER_CONTAINER_NAME);
        var fileData = BinaryData.fromString(TestUtils.getResourceFileContentAsString(TESTFILE_NAME));
        providerBlobHelper.uploadBlob(sourceContainer, fileData, "folder/" + TESTFILE_NAME);
        consumerBlobHelper.createContainer(CONSUMER_CONTAINER_NAME);

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
                        .add(EDC_NAMESPACE + "account", consumerAzuriteAccount.name())
                        .add(EDC_NAMESPACE + "container", CONSUMER_CONTAINER_NAME)
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
            assertThat(consumerBlobHelper.listBlobs(CONSUMER_CONTAINER_NAME))
                    .contains("%s/folder/%s".formatted(destfolder, TESTFILE_NAME));
        });
    }

    @Test
    void azureBlobPush_withoutDestFolder() {
        var assetId = "felix-blob-test-asset";

        Map<String, Object> dataAddress = Map.of(
                "name", "transfer-test",
                "@type", "DataAddress",
                "type", "AzureStorage",
                "container", PROVIDER_CONTAINER_NAME,
                "account", providerAzuriteAccount.name(),
                "blobPrefix", "folder/",
                "keyName", PROVIDER_KEY_ALIAS
        );

        // upload file to provider's blob store
        var sourceContainer = providerBlobHelper.createContainer(PROVIDER_CONTAINER_NAME);
        var fileData = BinaryData.fromString(TestUtils.getResourceFileContentAsString(TESTFILE_NAME));
        providerBlobHelper.uploadBlob(sourceContainer, fileData, "folder/" + TESTFILE_NAME);
        consumerBlobHelper.createContainer(CONSUMER_CONTAINER_NAME);

        // create objects in EDC
        provider().createAsset(assetId, Map.of(), dataAddress);
        var policyId = provider().createPolicyDefinition(bnpPolicy(consumer().getBpn()));
        provider().createContractDefinition(assetId, "def-1", policyId, policyId);

        var destination = createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "DataAddress")
                .add(EDC_NAMESPACE + "type", "AzureStorage")
                .add(EDC_NAMESPACE + "properties", createObjectBuilder()
                        .add(EDC_NAMESPACE + "type", "AzureStorage")
                        .add(EDC_NAMESPACE + "account", consumerAzuriteAccount.name())
                        .add(EDC_NAMESPACE + "container", CONSUMER_CONTAINER_NAME)
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
            assertThat(consumerBlobHelper.listBlobs(CONSUMER_CONTAINER_NAME))
                    .contains("folder/%s".formatted(TESTFILE_NAME));
        });
    }

    @Test
    void azureBlobPush_containerNotExist() {
        var assetId = "blob-test-asset";

        Map<String, Object> dataAddress = Map.of(
                "name", "transfer-test",
                "@type", "DataAddress",
                "type", "AzureStorage",
                "container", PROVIDER_CONTAINER_NAME,
                "account", providerAzuriteAccount.name(),
                "blobPrefix", "folder/",
                "keyName", PROVIDER_KEY_ALIAS
        );

        // upload file to provider's blob store
        var sourceContainer = providerBlobHelper.createContainer(PROVIDER_CONTAINER_NAME);
        var fileData = BinaryData.fromString(TestUtils.getResourceFileContentAsString(TESTFILE_NAME));
        providerBlobHelper.uploadBlob(sourceContainer, fileData, "folder/" + TESTFILE_NAME);
        // consumerBlobHelper.createContainer(CONSUMER_CONTAINER_NAME); <-- container is not created

        // create objects in EDC
        provider().createAsset(assetId, Map.of(), dataAddress);
        var policyId = provider().createPolicyDefinition(bnpPolicy(consumer().getBpn()));
        provider().createContractDefinition(assetId, "def-1", policyId, policyId);

        var destination = createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "DataAddress")
                .add(EDC_NAMESPACE + "type", "AzureStorage")
                .add(EDC_NAMESPACE + "properties", createObjectBuilder()
                        .add(EDC_NAMESPACE + "type", "AzureStorage")
                        .add(EDC_NAMESPACE + "account", consumerAzuriteAccount.name())
                        .add(EDC_NAMESPACE + "container", CONSUMER_CONTAINER_NAME)
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
            assertThat(consumerBlobHelper.listBlobs(CONSUMER_CONTAINER_NAME))
                    .contains("folder/%s".formatted(TESTFILE_NAME));
        });
    }

}
