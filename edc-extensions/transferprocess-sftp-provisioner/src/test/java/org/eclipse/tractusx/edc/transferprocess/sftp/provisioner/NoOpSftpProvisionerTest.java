/*
 * Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.edc.transferprocess.sftp.provisioner;

import java.util.concurrent.CompletableFuture;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.eclipse.edc.connector.transfer.spi.types.DeprovisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionResponse;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedContentResource;
import org.eclipse.edc.connector.transfer.spi.types.ResourceDefinition;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpDataAddress;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpLocation;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpUser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class NoOpSftpProvisionerTest {
  private final String policyScope = "scope";
  private final PolicyEngine policyEngine = Mockito.mock(PolicyEngine.class);
  private final NoOpSftpProvider sftpProvider = new NoOpSftpProvider();

  private final NoOpSftpProvisioner provisioner =
      new NoOpSftpProvisioner(policyScope, policyEngine, sftpProvider);

  @Test
  void canProvision__true() {
    String provisionType = "NoOp";
    SftpUser sftpUser = SftpUser.builder().name("name").password("password").build();
    SftpLocation sftpLocation = SftpLocation.builder().host("host").port(22).path("path").build();
    SftpDataAddress dataAddress = new SftpDataAddress(sftpUser, sftpLocation);
    SftpProviderResourceDefinition resourceDefinition =
        new SftpProviderResourceDefinition(provisionType, dataAddress);

    Assertions.assertTrue(provisioner.canProvision(resourceDefinition));
  }

  @Test
  void canProvision__falseProvisionType() {
    String provisionType = "AmazonS3";
    SftpUser sftpUser = SftpUser.builder().name("name").password("password").build();
    SftpLocation sftpLocation = SftpLocation.builder().host("host").port(22).path("path").build();
    SftpDataAddress dataAddress = new SftpDataAddress(sftpUser, sftpLocation);
    SftpProviderResourceDefinition resourceDefinition =
        new SftpProviderResourceDefinition(provisionType, dataAddress);

    Assertions.assertFalse(provisioner.canProvision(resourceDefinition));
  }

  @Test
  void canProvision__falseDefinitionType() {
    ResourceDefinition resourceDefinition = new WrongResourceDefinition();

    Assertions.assertFalse(provisioner.canProvision(resourceDefinition));
  }

  @Test
  void canDeprovision__true() {
    String provisionType = "NoOp";
    SftpUser sftpUser = SftpUser.builder().name("name").password("password").build();
    SftpLocation sftpLocation = SftpLocation.builder().host("host").port(22).path("path").build();
    Policy scopedPolicy = Mockito.mock(Policy.class);
    SftpDataAddress dataAddress = new SftpDataAddress(sftpUser, sftpLocation);
    String provisionedResourceID = "resource";
    SftpProvisionedContentResource provisionedContentResource =
        new SftpProvisionedContentResource(
            provisionType, scopedPolicy, dataAddress, provisionedResourceID);

    Assertions.assertTrue(provisioner.canDeprovision(provisionedContentResource));
  }

  @Test
  void canDeprovision__falseProvisionType() {
    String provisionType = "AmazonS3";
    SftpUser sftpUser = SftpUser.builder().name("name").password("password").build();
    SftpLocation sftpLocation = SftpLocation.builder().host("host").port(22).path("path").build();
    Policy scopedPolicy = Mockito.mock(Policy.class);
    SftpDataAddress dataAddress = new SftpDataAddress(sftpUser, sftpLocation);
    String provisionedResourceID = "resource";
    SftpProvisionedContentResource provisionedContentResource =
        new SftpProvisionedContentResource(
            provisionType, scopedPolicy, dataAddress, provisionedResourceID);

    Assertions.assertFalse(provisioner.canDeprovision(provisionedContentResource));
  }

  @Test
  void canDeprovision__falseDefinitionType() {
    ProvisionedContentResource provisionedContentResource = new WrongProvisionedContentResource();

    Assertions.assertFalse(provisioner.canDeprovision(provisionedContentResource));
  }

  @Test
  @SneakyThrows
  void provision__successful() {
    String provisionType = "NoOp";
    SftpUser sftpUser = SftpUser.builder().name("name").password("password").build();
    SftpLocation sftpLocation = SftpLocation.builder().host("host").port(22).path("path").build();
    SftpDataAddress dataAddress = new SftpDataAddress(sftpUser, sftpLocation);
    SftpProviderResourceDefinition resourceDefinition =
        new SftpProviderResourceDefinition(provisionType, dataAddress);
    Policy policy = Mockito.mock(Policy.class);

    Mockito.when(policyEngine.filter(policy, policyScope)).thenReturn(policy);

    CompletableFuture<StatusResult<ProvisionResponse>> future =
        provisioner.provision(resourceDefinition, policy);
    StatusResult<ProvisionResponse> result = future.get();

    Assertions.assertTrue(result.succeeded());
  }

  @Test
  @SneakyThrows
  void provision__failedWrongProvisionType() {
    String provisionType = "AmazonS3";
    SftpUser sftpUser = SftpUser.builder().name("name").password("password").build();
    SftpLocation sftpLocation = SftpLocation.builder().host("host").port(22).path("path").build();
    SftpDataAddress dataAddress = new SftpDataAddress(sftpUser, sftpLocation);
    SftpProviderResourceDefinition resourceDefinition =
        new SftpProviderResourceDefinition(provisionType, dataAddress);
    Policy policy = Mockito.mock(Policy.class);

    Mockito.when(policyEngine.filter(policy, policyScope)).thenReturn(policy);

    CompletableFuture<StatusResult<ProvisionResponse>> future =
        provisioner.provision(resourceDefinition, policy);
    StatusResult<ProvisionResponse> result = future.get();

    Assertions.assertTrue(result.failed());
  }

  @Test
  @SneakyThrows
  void deprovision__successful() {
    String provisionType = "NoOp";
    SftpUser sftpUser = SftpUser.builder().name("name").password("password").build();
    SftpLocation sftpLocation = SftpLocation.builder().host("host").port(22).path("path").build();
    SftpDataAddress dataAddress = new SftpDataAddress(sftpUser, sftpLocation);
    Policy policy = Mockito.mock(Policy.class);
    String provisionedResourceID = "resource";
    SftpProvisionedContentResource provisionedContentResource =
        new SftpProvisionedContentResource(
            provisionType, policy, dataAddress, provisionedResourceID);

    CompletableFuture<StatusResult<DeprovisionedResource>> future =
        provisioner.deprovision(provisionedContentResource, policy);
    StatusResult<DeprovisionedResource> result = future.get();

    Assertions.assertTrue(result.succeeded());
  }

  @Test
  @SneakyThrows
  void deprovision__failedWrongProvisionType() {
    String provisionType = "AmazonS3";
    SftpUser sftpUser = SftpUser.builder().name("name").password("password").build();
    SftpLocation sftpLocation = SftpLocation.builder().host("host").port(22).path("path").build();
    SftpDataAddress dataAddress = new SftpDataAddress(sftpUser, sftpLocation);
    Policy policy = Mockito.mock(Policy.class);
    String provisionedResourceID = "resource";
    SftpProvisionedContentResource provisionedContentResource =
        new SftpProvisionedContentResource(
            provisionType, policy, dataAddress, provisionedResourceID);

    CompletableFuture<StatusResult<DeprovisionedResource>> future =
        provisioner.deprovision(provisionedContentResource, policy);
    StatusResult<DeprovisionedResource> result = future.get();

    Assertions.assertTrue(result.failed());
  }

  @NoArgsConstructor
  private static class WrongResourceDefinition extends ResourceDefinition {
    @Override
    public <R extends ResourceDefinition, B extends Builder<R, B>> B toBuilder() {
      return null;
    }
  }

  @NoArgsConstructor
  private static class WrongProvisionedContentResource extends ProvisionedContentResource {}
}
