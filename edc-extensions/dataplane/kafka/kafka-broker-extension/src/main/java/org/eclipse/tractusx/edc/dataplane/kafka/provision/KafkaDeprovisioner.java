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
import org.eclipse.edc.connector.dataplane.spi.provision.Deprovisioner;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionResource;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.tractusx.edc.dataplane.kafka.acl.KafkaAclService;
import org.eclipse.tractusx.edc.dataplane.kafka.auth.KafkaOauthService;
import org.eclipse.tractusx.edc.dataplane.kafka.auth.OauthCredentials;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.eclipse.tractusx.edc.dataplane.kafka.dataaddress.KafkaBrokerDataAddressSchema.OAUTH_CLIENT_ID;
import static org.eclipse.tractusx.edc.dataplane.kafka.dataaddress.KafkaBrokerDataAddressSchema.OAUTH_CLIENT_SECRET_KEY;
import static org.eclipse.tractusx.edc.dataplane.kafka.dataaddress.KafkaBrokerDataAddressSchema.OAUTH_REVOKE_URL;
import static org.eclipse.tractusx.edc.dataplane.kafka.dataaddress.KafkaBrokerDataAddressSchema.OAUTH_TOKEN_URL;
import static org.eclipse.tractusx.edc.dataplane.kafka.provision.KafkaProvisionConstants.KAFKA_RESOURCE_TYPE;

/**
 * Data-plane {@link Deprovisioner} for {@code KafkaBroker} flows: revokes the broker ACLs and the OAuth2
 * token when the flow is deprovisioned on terminate, closing the access window independently of token
 * expiry. Cleanup is idempotent — a missing token or untracked ACLs is treated as success.
 */
public class KafkaDeprovisioner implements Deprovisioner {

    private final Vault vault;
    private final KafkaOauthService oauthService;
    @Nullable
    private final KafkaAclService aclService;
    private final Monitor monitor;

    public KafkaDeprovisioner(Vault vault, KafkaOauthService oauthService, @Nullable KafkaAclService aclService, Monitor monitor) {
        this.vault = vault;
        this.oauthService = oauthService;
        this.aclService = aclService;
        this.monitor = monitor;
    }

    @Override
    public String supportedType() {
        return KAFKA_RESOURCE_TYPE;
    }

    @Override
    public CompletableFuture<StatusResult<DeprovisionedResource>> deprovision(ProvisionResource resource) {
        try {
            var flowId = resource.getFlowId();

            if (aclService != null) {
                var aclResult = aclService.revokeAclsForTransferProcess(flowId);
                if (aclResult.failed()) {
                    return completed(StatusResult.failure(ResponseStatus.FATAL_ERROR, "Failed to revoke Kafka ACLs: " + aclResult.getFailureDetail()));
                }
            }

            var token = vault.resolveSecret(flowId);
            if (token != null) {
                oauthService.revokeToken(extractOauthCredentials(resource.getDataAddress()), token);
                vault.deleteSecret(flowId);
            }

            monitor.debug("Deprovisioned Kafka flow %s".formatted(flowId));
            return completed(StatusResult.success(DeprovisionedResource.Builder.newInstance()
                    .id(resource.getId())
                    .flowId(flowId)
                    .build()));
        } catch (Exception e) {
            return completed(StatusResult.failure(ResponseStatus.FATAL_ERROR, "Failed to deprovision Kafka data flow: " + e.getMessage()));
        }
    }

    private OauthCredentials extractOauthCredentials(DataAddress source) {
        var clientSecret = Optional.ofNullable(vault.resolveSecret(source.getStringProperty(OAUTH_CLIENT_SECRET_KEY)))
                .orElseThrow(() -> new EdcException("Kafka client secret was not found in the vault"));
        return new OauthCredentials(
                source.getStringProperty(OAUTH_TOKEN_URL),
                Optional.ofNullable(source.getStringProperty(OAUTH_REVOKE_URL)),
                source.getStringProperty(OAUTH_CLIENT_ID),
                clientSecret);
    }

    private static CompletableFuture<StatusResult<DeprovisionedResource>> completed(StatusResult<DeprovisionedResource> result) {
        return CompletableFuture.completedFuture(result);
    }
}
