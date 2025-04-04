/*
 * Copyright (c) 2025 Cofinity-X
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

package org.eclipse.edc.connector.provision.azure.blob;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ResourceDefinition;

import java.util.Objects;

@Deprecated(since = "0.9.0")
// TODO: remove after https://github.com/eclipse-edc/Technology-Azure/issues/338 has been fixed and the new EDC version is rolled out
@JsonDeserialize(builder = ObjectStorageResourceDefinition.Builder.class)
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

    @JsonPOJOBuilder(withPrefix = "")
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
