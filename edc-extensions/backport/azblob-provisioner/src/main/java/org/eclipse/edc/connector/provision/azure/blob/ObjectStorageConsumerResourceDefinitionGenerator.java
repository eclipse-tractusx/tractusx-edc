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

import org.eclipse.edc.azure.blob.AzureBlobStoreSchema;
import org.eclipse.edc.connector.controlplane.transfer.spi.provision.ConsumerResourceDefinitionGenerator;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ResourceDefinition;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.policy.model.Policy;
import org.jetbrains.annotations.Nullable;

import static java.util.UUID.randomUUID;

public class ObjectStorageConsumerResourceDefinitionGenerator implements ConsumerResourceDefinitionGenerator {

    @Override
    public @Nullable ResourceDefinition generate(TransferProcess transferProcess, Policy policy) {
        var destination = transferProcess.getDataDestination();
        var id = randomUUID().toString();
        var account = destination.getStringProperty(AzureBlobStoreSchema.ACCOUNT_NAME);
        var container = destination.getStringProperty(AzureBlobStoreSchema.CONTAINER_NAME);
        var folderName = destination.getStringProperty(AzureBlobStoreSchema.FOLDER_NAME);

        if (container == null) {
            container = randomUUID().toString();
        }
        return ObjectStorageResourceDefinition.Builder.newInstance()
                .id(id)
                .accountName(account)
                .containerName(container)
                .folderName(folderName)
                .build();
    }

    @Override
    public boolean canGenerate(TransferProcess dataRequest, Policy policy) {
        return AzureBlobStoreSchema.TYPE.equals(dataRequest.getDestinationType());
    }
}
