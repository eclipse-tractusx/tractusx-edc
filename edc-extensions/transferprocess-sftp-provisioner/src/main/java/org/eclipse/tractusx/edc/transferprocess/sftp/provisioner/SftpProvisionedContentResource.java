/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Initial API and Implementation
 *
 */

package org.eclipse.tractusx.edc.transferprocess.sftp.provisioner;

import org.eclipse.edc.connector.transfer.spi.types.ProvisionedContentResource;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpDataAddress;

import java.util.Objects;

public class SftpProvisionedContentResource extends ProvisionedContentResource {
    private String providerType;
    private Policy scopedPolicy;
    private SftpDataAddress sftpDataAddress;

    private SftpProvisionedContentResource() {

    }

    public String getProviderType() {
        return providerType;
    }

    public Policy getScopedPolicy() {
        return scopedPolicy;
    }

    public String getProvisionedResourceId() {
        return id;
    }

    public SftpDataAddress getSftpDataAddress() {
        return sftpDataAddress;
    }


    public static class Builder extends ProvisionedContentResource.Builder<SftpProvisionedContentResource, Builder> {

        protected Builder(SftpProvisionedContentResource resource) {
            super(resource);
        }

        public static Builder newInstance() {
            return new Builder(new SftpProvisionedContentResource());
        }

        public Builder providerType(String providerType) {
            this.provisionedResource.providerType = providerType;
            return this;
        }

        public Builder scopedPolicy(Policy scopedPolicy) {
            provisionedResource.scopedPolicy = scopedPolicy;
            return this;
        }

        public Builder sftpDataAddress(SftpDataAddress dataAddress) {
            dataAddress(dataAddress);
            provisionedResource.sftpDataAddress = dataAddress;
            return this;
        }

        public Builder provisionedResourceId(String resourceId) {
            id(resourceId);

            return this;
        }

        public SftpProvisionedContentResource build() {
            provisionedResource.dataAddress = dataAddressBuilder.build();
            Objects.requireNonNull(provisionedResource.providerType, "providerType");
            Objects.requireNonNull(provisionedResource.scopedPolicy, "scopedPolicy");
            Objects.requireNonNull(provisionedResource.sftpDataAddress, "dataAddress");
            return provisionedResource;
        }

    }
}
