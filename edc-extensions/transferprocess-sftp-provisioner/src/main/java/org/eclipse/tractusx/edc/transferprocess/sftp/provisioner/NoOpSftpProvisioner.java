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

import java.util.concurrent.CompletableFuture;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
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

@RequiredArgsConstructor
public class NoOpSftpProvisioner
    implements Provisioner<SftpProviderResourceDefinition, SftpProvisionedContentResource> {
  static final String DATA_ADDRESS_TYPE = "sftp";
  static final String PROVIDER_TYPE = "NoOp";

  @NonNull private final String policyScope;
  @NonNull private final PolicyEngine policyEngine;
  @NonNull private final NoOpSftpProvider sftpProvider;

  @Override
  public boolean canProvision(@NonNull ResourceDefinition resourceDefinition) {
    if (!(resourceDefinition instanceof SftpProviderResourceDefinition)) {
      return false;
    }
    if (!(((SftpProviderResourceDefinition) resourceDefinition)
        .getProviderType()
        .equals(PROVIDER_TYPE))) {
      return false;
    }
    try {
      SftpDataAddress.fromDataAddress(
          ((SftpProviderResourceDefinition) resourceDefinition).getSftpDataAddress());
    } catch (EdcSftpException e) {
      return false;
    }
    return true;
  }

  @Override
  public boolean canDeprovision(@NonNull ProvisionedResource provisionedResource) {
    if (!(provisionedResource instanceof SftpProvisionedContentResource)) {
      return false;
    }

    if (!(((SftpProvisionedContentResource) provisionedResource)
        .getProviderType()
        .equals(PROVIDER_TYPE))) {
      return false;
    }

    try {
      SftpDataAddress.fromDataAddress(
          ((SftpProvisionedContentResource) provisionedResource).getSftpDataAddress());
    } catch (EdcSftpException e) {
      return false;
    }
    return true;
  }

  @Override
  public CompletableFuture<StatusResult<ProvisionResponse>> provision(
      SftpProviderResourceDefinition sftpProviderResourceDefinition, Policy policy) {

    return CompletableFuture.supplyAsync(
        () -> {
          if (!this.canProvision(sftpProviderResourceDefinition)) {
            return StatusResult.failure(ResponseStatus.FATAL_ERROR);
          }
          // As of the time of writing, policies don't actually do anything in this context.
          // They are included here in case EDC wants to use them eventually.
          Policy scopedPolicy;
          scopedPolicy = policyEngine.filter(policy, policyScope);
          sftpProvider.createLocation(
              sftpProviderResourceDefinition.getSftpDataAddress().getSftpLocation());
          sftpProvider.createUser(
              sftpProviderResourceDefinition.getSftpDataAddress().getSftpUser());

          SftpProvisionedContentResource sftpProvisionedContentResource =
              SftpProvisionedContentResource.builder()
                  .sftpDataAddress(sftpProviderResourceDefinition.getSftpDataAddress())
                  .providerType(PROVIDER_TYPE)
                  .scopedPolicy(scopedPolicy)
                  .provisionedResourceId(
                      generateResourceId(
                          sftpProviderResourceDefinition.getSftpDataAddress().getSftpUser(),
                          sftpProviderResourceDefinition.getSftpDataAddress().getSftpLocation()))
                  .build();

          return StatusResult.success(
              ProvisionResponse.Builder.newInstance()
                  .resource(sftpProvisionedContentResource)
                  .build());
        });
  }

  @Override
  public CompletableFuture<StatusResult<DeprovisionedResource>> deprovision(
      SftpProvisionedContentResource sftpProvisionedContentResource, Policy policy) {
    return CompletableFuture.supplyAsync(
        () -> {
          if (!this.canDeprovision(sftpProvisionedContentResource)) {
            return StatusResult.failure(ResponseStatus.FATAL_ERROR);
          }
          // As of the time of writing, policies don't actually do anything in this context.
          // They are included here in case EDC wants to use them eventually.
          sftpProvider.deleteLocation(
              sftpProvisionedContentResource.getSftpDataAddress().getSftpLocation());
          sftpProvider.deleteUser(
              sftpProvisionedContentResource.getSftpDataAddress().getSftpUser());

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
