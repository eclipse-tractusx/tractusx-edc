/********************************************************************************
 * Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
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

package org.eclipse.tractusx.edc.transferprocess.sftp.provisioner;

import org.eclipse.edc.connector.transfer.spi.provision.Provisioner;
import org.eclipse.edc.connector.transfer.spi.types.DeprovisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionResponse;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.ResourceDefinition;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.EdcSftpException;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpDataAddress;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpLocation;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpUser;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class NoOpSftpProvisioner implements Provisioner<SftpProviderResourceDefinition, SftpProvisionedContentResource> {
    public static final String DATA_ADDRESS_TYPE = "sftp";
    public static final String PROVIDER_TYPE = "NoOp";

    private final String policyScope;
    private final PolicyEngine policyEngine;
    private final NoOpSftpProvider sftpProvider;

    public NoOpSftpProvisioner(String policyScope, PolicyEngine policyEngine, NoOpSftpProvider sftpProvider) {
        this.policyScope = policyScope;
        this.policyEngine = policyEngine;
        this.sftpProvider = sftpProvider;
    }

    @Override
    public boolean canProvision(ResourceDefinition resourceDefinition) {
        Objects.requireNonNull(resourceDefinition, "resourceDefinition");
        if (!(resourceDefinition instanceof SftpProviderResourceDefinition)) {
            return false;
        }
        if (!(((SftpProviderResourceDefinition) resourceDefinition)
                .getProviderType()
                .equals(PROVIDER_TYPE))) {
            return false;
        }
        try {
            SftpDataAddress.fromDataAddress(((SftpProviderResourceDefinition) resourceDefinition).getSftpDataAddress());
        } catch (EdcSftpException e) {
            return false;
        }
        return true;
    }

    @Override
    public boolean canDeprovision(ProvisionedResource provisionedResource) {
        Objects.requireNonNull(provisionedResource, "provisionedResource");
        if (!(provisionedResource instanceof SftpProvisionedContentResource)) {
            return false;
        }

        if (!(((SftpProvisionedContentResource) provisionedResource)
                .getProviderType()
                .equals(PROVIDER_TYPE))) {
            return false;
        }

        try {
            SftpDataAddress.fromDataAddress(((SftpProvisionedContentResource) provisionedResource).getDataAddress());
        } catch (EdcSftpException e) {
            return false;
        }
        return true;
    }

    @Override
    public CompletableFuture<StatusResult<ProvisionResponse>> provision(SftpProviderResourceDefinition sftpProviderResourceDefinition, Policy policy) {

        return CompletableFuture.supplyAsync(
                () -> {
                    if (!this.canProvision(sftpProviderResourceDefinition)) {
                        return StatusResult.failure(ResponseStatus.FATAL_ERROR);
                    }
                    // As of the time of writing, policies don't actually do anything in this context.
                    // They are included here in case EDC wants to use them eventually.
                    Policy scopedPolicy;
                    scopedPolicy = policyEngine.filter(policy, policyScope);
                    sftpProvider.createLocation(sftpProviderResourceDefinition.getSftpDataAddress().getSftpLocation());
                    sftpProvider.createUser(sftpProviderResourceDefinition.getSftpDataAddress().getSftpUser());

                    var randomId = UUID.randomUUID().toString();
                    var sftpProvisionedContentResource = SftpProvisionedContentResource.Builder.newInstance()
                            .sftpDataAddress(sftpProviderResourceDefinition.getSftpDataAddress())
                            .providerType(PROVIDER_TYPE)
                            .scopedPolicy(scopedPolicy)
                            .provisionedResourceId(randomId)
                            .resourceDefinitionId(sftpProviderResourceDefinition.getId())
                            .provisionedResourceId(
                                    generateResourceId(
                                            sftpProviderResourceDefinition.getSftpDataAddress().getSftpUser(),
                                            sftpProviderResourceDefinition.getSftpDataAddress().getSftpLocation()))
                            .build();

                    return StatusResult.success(ProvisionResponse.Builder.newInstance()
                            .resource(sftpProvisionedContentResource)
                            .build());
                });
    }

    @Override
    public CompletableFuture<StatusResult<DeprovisionedResource>> deprovision(SftpProvisionedContentResource sftpProvisionedContentResource, Policy policy) {
        return CompletableFuture.supplyAsync(
                () -> {
                    if (!this.canDeprovision(sftpProvisionedContentResource)) {
                        return StatusResult.failure(ResponseStatus.FATAL_ERROR);
                    }
                    // As of the time of writing, policies don't actually do anything in this context.
                    // They are included here in case EDC wants to use them eventually.
                    var dataAddress = sftpProvisionedContentResource.getSftpDataAddress();
                    sftpProvider.deleteLocation(dataAddress.getSftpLocation());
                    sftpProvider.deleteUser(dataAddress.getSftpUser());

                    DeprovisionedResource deprovisionedResource =
                            DeprovisionedResource.Builder.newInstance()
                                    .provisionedResourceId(sftpProvisionedContentResource.getProvisionedResourceId())
                                    .inProcess(true)
                                    .build();

                    return StatusResult.success(deprovisionedResource);
                });
    }

    private String generateResourceId(SftpUser sftpUser, SftpLocation sftpLocation) {
        return String.format(
                "%s@%s:%d/%s",
                sftpUser.getName(), sftpLocation.getHost(), sftpLocation.getPort(), sftpLocation.getPath());
    }
}
