/********************************************************************************
 * Copyright (c) 2020,2021 Microsoft Corporation
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

package org.eclipse.edc.connector.provision.azure.blob;

import dev.failsafe.RetryPolicy;
import org.eclipse.edc.azure.blob.AzureSasToken;
import org.eclipse.edc.azure.blob.api.BlobStoreApi;
import org.eclipse.edc.connector.controlplane.transfer.spi.provision.Provisioner;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DeprovisionedResource;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ProvisionResponse;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ProvisionedResource;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ResourceDefinition;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.StatusResult;
import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;

import static dev.failsafe.Failsafe.with;

public class ObjectStorageProvisioner implements Provisioner<ObjectStorageResourceDefinition, ObjectContainerProvisionedResource> {
    private final RetryPolicy<Object> retryPolicy;
    private final Monitor monitor;
    private final BlobStoreApi blobStoreApi;

    public ObjectStorageProvisioner(RetryPolicy<Object> retryPolicy, Monitor monitor, BlobStoreApi blobStoreApi) {
        this.retryPolicy = retryPolicy;
        this.monitor = monitor;
        this.blobStoreApi = blobStoreApi;
    }

    @Override
    public boolean canProvision(ResourceDefinition resourceDefinition) {
        return resourceDefinition instanceof ObjectStorageResourceDefinition;
    }

    @Override
    public boolean canDeprovision(ProvisionedResource resourceDefinition) {
        return resourceDefinition instanceof ObjectContainerProvisionedResource;
    }

    @Override
    public CompletableFuture<StatusResult<ProvisionResponse>> provision(ObjectStorageResourceDefinition resourceDefinition, Policy policy) {
        String containerName = resourceDefinition.getContainerName();
        String accountName = resourceDefinition.getAccountName();
        String folderName = resourceDefinition.getFolderName();

        monitor.debug("Azure Storage Container request submitted: " + containerName);

        OffsetDateTime expiryTime = OffsetDateTime.now().plusHours(1);

        return with(retryPolicy).getAsync(() -> blobStoreApi.exists(accountName, containerName))
                .thenCompose(exists -> {
                    if (exists) {
                        return reusingExistingContainer(containerName);
                    } else {
                        return createContainer(containerName, accountName);
                    }
                })
                .thenCompose(empty -> createContainerSasToken(containerName, accountName, expiryTime))
                .thenApply(writeOnlySas -> {
                    // Ensure resource name is unique to avoid key collisions in local and remote vaults
                    String resourceName = resourceDefinition.getId() + "-container";
                    var resource = ObjectContainerProvisionedResource.Builder.newInstance()
                            .id(containerName)
                            .accountName(accountName)
                            .containerName(containerName)
                            .folderName(folderName)
                            .resourceDefinitionId(resourceDefinition.getId())
                            .transferProcessId(resourceDefinition.getTransferProcessId())
                            .resourceName(resourceName)
                            .hasToken(true)
                            .build();

                    var secretToken = new AzureSasToken("?" + writeOnlySas, expiryTime.toInstant().toEpochMilli());

                    var response = ProvisionResponse.Builder.newInstance().resource(resource).secretToken(secretToken).build();
                    return StatusResult.success(response);
                });
    }

    @Override
    public CompletableFuture<StatusResult<DeprovisionedResource>> deprovision(ObjectContainerProvisionedResource provisionedResource, Policy policy) {
        return with(retryPolicy).runAsync(() -> blobStoreApi.deleteContainer(provisionedResource.getAccountName(), provisionedResource.getContainerName()))
                //the sas token will expire automatically. there is no way of revoking them other than a stored access policy
                .thenApply(empty -> StatusResult.success(DeprovisionedResource.Builder.newInstance().provisionedResourceId(provisionedResource.getId()).build()));
    }

    @NotNull
    private CompletableFuture<Void> reusingExistingContainer(String containerName) {
        monitor.debug("ObjectStorageProvisioner: re-use existing container " + containerName);
        return CompletableFuture.completedFuture(null);
    }

    @NotNull
    private CompletableFuture<Void> createContainer(String containerName, String accountName) {
        return with(retryPolicy)
                .runAsync(() -> {
                    blobStoreApi.createContainer(accountName, containerName);
                    monitor.debug("ObjectStorageProvisioner: created a new container " + containerName);
                });
    }

    @NotNull
    private CompletableFuture<String> createContainerSasToken(String containerName, String accountName, OffsetDateTime expiryTime) {
        return with(retryPolicy)
                .getAsync(() -> {
                    monitor.debug("ObjectStorageProvisioner: obtained temporary SAS token (write-only)");
                    return blobStoreApi.createContainerSasToken(accountName, containerName, "w", expiryTime);
                });
    }
}
