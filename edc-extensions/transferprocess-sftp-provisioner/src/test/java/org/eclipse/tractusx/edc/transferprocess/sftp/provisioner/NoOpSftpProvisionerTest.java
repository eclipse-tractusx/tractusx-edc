/********************************************************************************
 * Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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

import org.eclipse.edc.connector.transfer.spi.types.ProvisionedContentResource;
import org.eclipse.edc.connector.transfer.spi.types.ResourceDefinition;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpDataAddress;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpLocation;
import org.eclipse.tractusx.edc.transferprocess.sftp.common.SftpUser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NoOpSftpProvisionerTest {
    private final String policyScope = "scope";
    private final PolicyEngine policyEngine = mock(PolicyEngine.class);
    private final NoOpSftpProvider sftpProvider = new NoOpSftpProvider();

    private final NoOpSftpProvisioner provisioner =
            new NoOpSftpProvisioner(policyScope, policyEngine, sftpProvider);

    @Test
    void canProvision_true() {
        var provisionType = "NoOp";
        var sftpUser = SftpUser.Builder.newInstance().name("name").password("password").build();
        var sftpLocation = SftpLocation.Builder.newInstance().host("host").port(22).path("path").build();
        var dataAddress = SftpDataAddress.Builder.newInstance().sftpUser(sftpUser).sftpLocation(sftpLocation).build();
        var resourceDefinition =
                new SftpProviderResourceDefinition(provisionType, dataAddress);

        Assertions.assertTrue(provisioner.canProvision(resourceDefinition));
    }

    @Test
    void canProvision_falseProvisionType() {
        var provisionType = "AmazonS3";
        SftpUser sftpUser = SftpUser.Builder.newInstance().name("name").password("password").build();
        SftpLocation sftpLocation = SftpLocation.Builder.newInstance().host("host").port(22).path("path").build();
        var dataAddress = SftpDataAddress.Builder.newInstance().sftpUser(sftpUser).sftpLocation(sftpLocation).build();
        var resourceDefinition =
                new SftpProviderResourceDefinition(provisionType, dataAddress);

        Assertions.assertFalse(provisioner.canProvision(resourceDefinition));
    }

    @Test
    void canProvision_falseDefinitionType() {
        ResourceDefinition resourceDefinition = new WrongResourceDefinition();

        Assertions.assertFalse(provisioner.canProvision(resourceDefinition));
    }

    @Test
    void canDeprovision_true() {
        var provisionType = "NoOp";
        var sftpUser = SftpUser.Builder.newInstance().name("name").password("password").build();
        var sftpLocation = SftpLocation.Builder.newInstance().host("host").port(22).path("path").build();
        var scopedPolicy = mock(Policy.class);
        var dataAddress = SftpDataAddress.Builder.newInstance().sftpUser(sftpUser).sftpLocation(sftpLocation).build();
        var provisionedResourceId = "resource";
        var provisionedContentResource =
                SftpProvisionedContentResource.Builder.newInstance()
                        .providerType(provisionType)
                        .scopedPolicy(scopedPolicy)
                        .transferProcessId(UUID.randomUUID().toString())
                        .resourceDefinitionId(UUID.randomUUID().toString())
                        .resourceName("test-resource")
                        .provisionedResourceId("test-resdef-id")
                        .sftpDataAddress(dataAddress)
                        .provisionedResourceId(provisionedResourceId)
                        .build();

        assertThat(provisioner.canDeprovision(provisionedContentResource)).isTrue();
    }

    @Test
    void canDeprovision_falseProvisionType() {
        var provisionType = "AmazonS3";
        SftpUser sftpUser = SftpUser.Builder.newInstance().name("name").password("password").build();
        SftpLocation sftpLocation = SftpLocation.Builder.newInstance().host("host").port(22).path("path").build();
        var scopedPolicy = mock(Policy.class);
        var dataAddress = SftpDataAddress.Builder.newInstance().sftpUser(sftpUser).sftpLocation(sftpLocation).build();
        var provisionedResourceId = "resource";
        var provisionedContentResource =
                SftpProvisionedContentResource.Builder.newInstance()
                        .providerType(provisionType)
                        .scopedPolicy(scopedPolicy)
                        .transferProcessId(UUID.randomUUID().toString())
                        .resourceDefinitionId(UUID.randomUUID().toString())
                        .resourceName("test-resource")
                        .provisionedResourceId("test-resdef-id")
                        .sftpDataAddress(dataAddress).provisionedResourceId(provisionedResourceId)
                        .build();

        Assertions.assertFalse(provisioner.canDeprovision(provisionedContentResource));
    }

    @Test
    void canDeprovision_falseDefinitionType() {
        ProvisionedContentResource provisionedContentResource = new WrongProvisionedContentResource();

        Assertions.assertFalse(provisioner.canDeprovision(provisionedContentResource));
    }

    @Test
    void provision_successful() throws ExecutionException, InterruptedException {
        var provisionType = "NoOp";
        SftpUser sftpUser = SftpUser.Builder.newInstance().name("name").password("password").build();
        SftpLocation sftpLocation = SftpLocation.Builder.newInstance().host("host").port(22).path("path").build();
        var dataAddress = SftpDataAddress.Builder.newInstance().type("sftp").sftpUser(sftpUser).sftpLocation(sftpLocation).build();
        var resourceDefinition = new SftpProviderResourceDefinition(provisionType, dataAddress);
        var policy = mock(Policy.class);

        when(policyEngine.filter(policy, policyScope)).thenReturn(policy);

        var future = provisioner.provision(resourceDefinition, policy);
        assertThat(future).succeedsWithin(ofSeconds(5));
        assertThat(future).isCompletedWithValueMatching(AbstractResult::succeeded);
    }

    @Test
    void provision_failedWrongProvisionType() {
        var provisionType = "AmazonS3";
        SftpUser sftpUser = SftpUser.Builder.newInstance().name("name").password("password").build();
        SftpLocation sftpLocation = SftpLocation.Builder.newInstance().host("host").port(22).path("path").build();
        var dataAddress = SftpDataAddress.Builder.newInstance().sftpUser(sftpUser).sftpLocation(sftpLocation).build();
        var resourceDefinition =
                new SftpProviderResourceDefinition(provisionType, dataAddress);
        var policy = mock(Policy.class);

        when(policyEngine.filter(policy, policyScope)).thenReturn(policy);

        var future =
                provisioner.provision(resourceDefinition, policy);

        assertThat(future).succeedsWithin(ofSeconds(5));
        assertThat(future).isCompletedWithValueMatching(AbstractResult::failed);
    }

    @Test
    void deprovision_successful() {
        var provisionType = "NoOp";
        var sftpUser = SftpUser.Builder.newInstance().name("name").password("password").build();
        var sftpLocation = SftpLocation.Builder.newInstance().host("host").port(22).path("path").build();
        var dataAddress = SftpDataAddress.Builder.newInstance().sftpUser(sftpUser).sftpLocation(sftpLocation).build();
        var policy = mock(Policy.class);
        var provisionedResourceId = "resource";
        var provisionedContentResource = SftpProvisionedContentResource.Builder.newInstance()
                .providerType(provisionType)
                .scopedPolicy(policy)
                .sftpDataAddress(dataAddress)
                .provisionedResourceId(provisionedResourceId)
                .transferProcessId(UUID.randomUUID().toString())
                .resourceDefinitionId(UUID.randomUUID().toString())
                .resourceName("test-resource")
                .provisionedResourceId("test-resdef-id")
                .build();
        var future = provisioner.deprovision(provisionedContentResource, policy);

        assertThat(future).succeedsWithin(ofSeconds(5));
        assertThat(future).isCompletedWithValueMatching(AbstractResult::succeeded);

    }

    @Test
    void deprovision_failedWrongProvisionType() {
        var provisionType = "AmazonS3";
        var sftpUser = SftpUser.Builder.newInstance().name("name").password("password").build();
        var sftpLocation = SftpLocation.Builder.newInstance().host("host").port(22).path("path").build();
        var dataAddress = SftpDataAddress.Builder.newInstance().sftpUser(sftpUser).sftpLocation(sftpLocation).build();
        var policy = mock(Policy.class);
        var provisionedResourceId = "resource";
        var provisionedContentResource =
                SftpProvisionedContentResource.Builder.newInstance()
                        .providerType(provisionType)
                        .scopedPolicy(policy)
                        .sftpDataAddress(dataAddress).provisionedResourceId(provisionedResourceId)
                        .transferProcessId(UUID.randomUUID().toString())
                        .resourceDefinitionId(UUID.randomUUID().toString())
                        .resourceName("test-resource")
                        .provisionedResourceId("test-resdef-id")
                        .build();

        var future =
                provisioner.deprovision(provisionedContentResource, policy);
        assertThat(future).isCompletedWithValueMatching(AbstractResult::failed);
    }

    private static class WrongResourceDefinition extends ResourceDefinition {
        @Override
        public <R extends ResourceDefinition, B extends Builder<R, B>> B toBuilder() {
            return null;
        }
    }

    private static class WrongProvisionedContentResource extends ProvisionedContentResource {
    }
}
