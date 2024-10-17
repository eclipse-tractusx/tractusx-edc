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
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.common.StorageSharedKeyCredential;

import java.util.List;


/**
 * Helper class that internally uses Azure SDK classes to create containers, upload blobs, generate SAS tokens, etc.
 */
public class AzureBlobHelper {
    private final String accountName;
    private final String key;
    private final String host;
    private final int port;
    private BlobServiceClient blobServiceClient;

    AzureBlobHelper(String accountName, String key, String host, int port) {
        this.accountName = accountName;
        this.key = key;
        this.host = host;
        this.port = port;
    }

    public BlobContainerClient createContainer(String containerName) {
        return blobClient().createBlobContainer(containerName);
    }

    public void uploadBlob(BlobContainerClient client, BinaryData data, String targetBlobName) {
        client.getBlobClient(targetBlobName).upload(data, true);
    }

    public List<String> listBlobs(String container) {
        if (blobClient().listBlobContainers().stream().noneMatch(bci -> bci.getName().equalsIgnoreCase(container))) {
            return List.of();
        }
        return blobClient()
                .getBlobContainerClient(container)
                .listBlobs()
                .stream().map(BlobItem::getName)
                .toList();
    }

    private BlobServiceClient blobClient() {
        if (blobServiceClient == null) {
            var endpoint = "http://%s:%s/%s".formatted(host, port, accountName);
            blobServiceClient = new BlobServiceClientBuilder()
                    .credential(new StorageSharedKeyCredential(accountName, key))
                    .endpoint(endpoint)
                    .buildClient();
        }
        return blobServiceClient;
    }
}
