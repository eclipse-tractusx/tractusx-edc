/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - add toBuilder method
 *
 */

package org.eclipse.edc.connector.provision.azure.blob;


import org.eclipse.edc.connector.controlplane.transfer.spi.types.ResourceDefinition;

import java.util.Objects;

public class ObjectStorageResourceDefinition extends ResourceDefinition {

    private String containerName;
    private String accountName;
    private String folderName;

    public String getContainerName() {
        return containerName;
    }

    public String getAccountName() {
        return accountName;
    }

    @Override
    public Builder toBuilder() {
        return initializeBuilder(new Builder())
                .containerName(containerName)
                .folderName(folderName)
                .accountName(accountName);
    }

    public String getFolderName() {
        return folderName;
    }

    public static class Builder extends ResourceDefinition.Builder<ObjectStorageResourceDefinition, Builder> {

        private Builder() {
            super(new ObjectStorageResourceDefinition());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder containerName(String id) {
            resourceDefinition.containerName = id;
            return this;
        }

        public Builder accountName(String accountName) {
            resourceDefinition.accountName = accountName;
            return this;
        }

        public Builder folderName(String folderName) {
            resourceDefinition.folderName = folderName;
            return this;
        }

        @Override
        protected void verify() {
            super.verify();
            Objects.requireNonNull(resourceDefinition.containerName, "containerName");
            Objects.requireNonNull(resourceDefinition.accountName, "accountName");
        }
    }

}
