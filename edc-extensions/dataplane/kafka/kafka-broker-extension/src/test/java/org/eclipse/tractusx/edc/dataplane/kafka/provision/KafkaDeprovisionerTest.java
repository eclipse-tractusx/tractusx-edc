/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.edc.dataplane.kafka.provision;

import org.eclipse.edc.connector.dataplane.spi.provision.DeprovisionedResource;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionResource;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.tractusx.edc.dataplane.kafka.acl.KafkaAclService;
import org.eclipse.tractusx.edc.dataplane.kafka.auth.KafkaOauthService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.dataplane.kafka.dataaddress.KafkaBrokerDataAddressSchema.KAFKA_TYPE;
import static org.eclipse.tractusx.edc.dataplane.kafka.dataaddress.KafkaBrokerDataAddressSchema.OAUTH_CLIENT_ID;
import static org.eclipse.tractusx.edc.dataplane.kafka.dataaddress.KafkaBrokerDataAddressSchema.OAUTH_CLIENT_SECRET_KEY;
import static org.eclipse.tractusx.edc.dataplane.kafka.dataaddress.KafkaBrokerDataAddressSchema.OAUTH_TOKEN_URL;
import static org.eclipse.tractusx.edc.dataplane.kafka.provision.KafkaProvisionConstants.KAFKA_RESOURCE_TYPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaDeprovisionerTest {

    private static final String FLOW_ID = "flow-1";
    private static final String SECRET_KEY = "client-secret-key";
    private static final String TOKEN_VALUE = "minted-token";

    private final Vault vault = mock();
    private final KafkaOauthService oauthService = mock();
    private final KafkaAclService aclService = mock();
    private final KafkaDeprovisioner deprovisioner = new KafkaDeprovisioner(vault, oauthService, aclService, mock(Monitor.class));

    @Test
    void deprovision_revokesAcls_token_andDeletesVaultSecret() throws Exception {
        when(aclService.revokeAclsForTransferProcess(FLOW_ID)).thenReturn(Result.success());
        when(vault.resolveSecret(FLOW_ID)).thenReturn(TOKEN_VALUE);
        when(vault.resolveSecret(SECRET_KEY)).thenReturn("secret-value");

        StatusResult<DeprovisionedResource> result = deprovisioner.deprovision(resource()).get();

        assertThat(result.succeeded()).isTrue();
        verify(aclService).revokeAclsForTransferProcess(FLOW_ID);
        verify(oauthService).revokeToken(any(), eq(TOKEN_VALUE));
        verify(vault).deleteSecret(FLOW_ID);
    }

    @Test
    void deprovision_isIdempotent_whenTokenAlreadyGone() throws Exception {
        when(aclService.revokeAclsForTransferProcess(FLOW_ID)).thenReturn(Result.success());
        when(vault.resolveSecret(FLOW_ID)).thenReturn(null);

        StatusResult<DeprovisionedResource> result = deprovisioner.deprovision(resource()).get();

        assertThat(result.succeeded()).isTrue();
        verify(oauthService, never()).revokeToken(any(), any());
        verify(vault, never()).deleteSecret(any());
    }

    @Test
    void deprovision_revokesToken_whenAclManagementDisabled() throws Exception {
        var noAcl = new KafkaDeprovisioner(vault, oauthService, null, mock(Monitor.class));
        when(vault.resolveSecret(FLOW_ID)).thenReturn(TOKEN_VALUE);
        when(vault.resolveSecret(SECRET_KEY)).thenReturn("secret-value");

        StatusResult<DeprovisionedResource> result = noAcl.deprovision(resource()).get();

        assertThat(result.succeeded()).isTrue();
        verify(oauthService).revokeToken(any(), eq(TOKEN_VALUE));
        verify(vault).deleteSecret(FLOW_ID);
    }

    @Test
    void deprovision_fails_whenAclRevocationFails() throws Exception {
        when(aclService.revokeAclsForTransferProcess(FLOW_ID)).thenReturn(Result.failure("broker error"));

        StatusResult<DeprovisionedResource> result = deprovisioner.deprovision(resource()).get();

        assertThat(result.failed()).isTrue();
        verify(oauthService, never()).revokeToken(any(), any());
    }

    @Test
    void deprovision_fails_whenClientSecretMissing() throws Exception {
        when(aclService.revokeAclsForTransferProcess(FLOW_ID)).thenReturn(Result.success());
        when(vault.resolveSecret(FLOW_ID)).thenReturn(TOKEN_VALUE);
        when(vault.resolveSecret(SECRET_KEY)).thenReturn(null);

        StatusResult<DeprovisionedResource> result = deprovisioner.deprovision(resource()).get();

        assertThat(result.failed()).isTrue();
    }

    private ProvisionResource resource() {
        var source = DataAddress.Builder.newInstance()
                .type(KAFKA_TYPE)
                .property(OAUTH_TOKEN_URL, "http://localhost:8080/token")
                .property(OAUTH_CLIENT_ID, "client-id")
                .property(OAUTH_CLIENT_SECRET_KEY, SECRET_KEY)
                .build();
        return ProvisionResource.Builder.newInstance()
                .id("resource-1")
                .flowId(FLOW_ID)
                .type(KAFKA_RESOURCE_TYPE)
                .dataAddress(source)
                .build();
    }
}
