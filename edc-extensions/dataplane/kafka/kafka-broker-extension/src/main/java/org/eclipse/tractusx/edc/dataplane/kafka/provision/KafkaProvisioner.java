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

import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionResource;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionedResource;
import org.eclipse.edc.connector.dataplane.spi.provision.Provisioner;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.tractusx.edc.dataplane.kafka.auth.KafkaOauthService;
import org.eclipse.tractusx.edc.dataplane.kafka.auth.OauthCredentials;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.eclipse.tractusx.edc.dataplane.kafka.dataaddress.KafkaBrokerDataAddressSchema.BOOTSTRAP_SERVERS;
import static org.eclipse.tractusx.edc.dataplane.kafka.dataaddress.KafkaBrokerDataAddressSchema.GROUP_PREFIX;
import static org.eclipse.tractusx.edc.dataplane.kafka.dataaddress.KafkaBrokerDataAddressSchema.KAFKA_TYPE;
import static org.eclipse.tractusx.edc.dataplane.kafka.dataaddress.KafkaBrokerDataAddressSchema.MECHANISM;
import static org.eclipse.tractusx.edc.dataplane.kafka.dataaddress.KafkaBrokerDataAddressSchema.OAUTH_CLIENT_ID;
import static org.eclipse.tractusx.edc.dataplane.kafka.dataaddress.KafkaBrokerDataAddressSchema.OAUTH_CLIENT_SECRET_KEY;
import static org.eclipse.tractusx.edc.dataplane.kafka.dataaddress.KafkaBrokerDataAddressSchema.OAUTH_REVOKE_URL;
import static org.eclipse.tractusx.edc.dataplane.kafka.dataaddress.KafkaBrokerDataAddressSchema.OAUTH_TOKEN_URL;
import static org.eclipse.tractusx.edc.dataplane.kafka.dataaddress.KafkaBrokerDataAddressSchema.POLL_DURATION;
import static org.eclipse.tractusx.edc.dataplane.kafka.dataaddress.KafkaBrokerDataAddressSchema.PROTOCOL;
import static org.eclipse.tractusx.edc.dataplane.kafka.dataaddress.KafkaBrokerDataAddressSchema.TOKEN;
import static org.eclipse.tractusx.edc.dataplane.kafka.dataaddress.KafkaBrokerDataAddressSchema.TOPIC;
import static org.eclipse.tractusx.edc.dataplane.kafka.provision.KafkaProvisionConstants.CONSUMER_GROUP_PREFIX_PROPERTY;
import static org.eclipse.tractusx.edc.dataplane.kafka.provision.KafkaProvisionConstants.DEFAULT_POLL_DURATION;
import static org.eclipse.tractusx.edc.dataplane.kafka.provision.KafkaProvisionConstants.KAFKA_RESOURCE_TYPE;

/**
 * Data-plane {@link Provisioner} for the {@code KafkaBroker} source type. On provision it mints a fresh
 * OAuth2 access token (Client Credentials flow) and builds the provisioned {@link DataAddress} — the EDR
 * handed to the consumer: the broker coordinates, topic, security settings, consumer-group prefix and the
 * minted token. The token is also stored in the vault keyed by the data-flow id so
 * {@link KafkaDeprovisioner} can revoke it on terminate. Broker ACLs are managed per activation by
 * {@code KafkaEndpointDataReferenceService} (so suspend/resume toggle access), not here.
 */
public class KafkaProvisioner implements Provisioner {

    private final Vault vault;
    private final KafkaOauthService oauthService;
    private final Monitor monitor;

    public KafkaProvisioner(Vault vault, KafkaOauthService oauthService, Monitor monitor) {
        this.vault = vault;
        this.oauthService = oauthService;
        this.monitor = monitor;
    }

    @Override
    public String supportedType() {
        return KAFKA_RESOURCE_TYPE;
    }

    @Override
    public CompletableFuture<StatusResult<ProvisionedResource>> provision(ProvisionResource resource) {
        try {
            var flowId = resource.getFlowId();
            var source = resource.getDataAddress();

            var groupPrefix = Optional.ofNullable(source.getStringProperty(GROUP_PREFIX))
                    .orElse((String) resource.getProperty(CONSUMER_GROUP_PREFIX_PROPERTY));
            if (groupPrefix == null || groupPrefix.isBlank()) {
                return completed(StatusResult.failure(ResponseStatus.FATAL_ERROR,
                        "Cannot determine Kafka consumer-group prefix: neither the '%s' property nor a consumer participant id is present"
                                .formatted(GROUP_PREFIX)));
            }

            var token = oauthService.getAccessToken(extractOauthCredentials(source));
            vault.storeSecret(flowId, token);

            var pollDuration = Optional.ofNullable(source.getStringProperty(POLL_DURATION)).orElse(DEFAULT_POLL_DURATION);

            var edr = DataAddress.Builder.newInstance()
                    .type(KAFKA_TYPE)
                    .property(BOOTSTRAP_SERVERS, source.getStringProperty(BOOTSTRAP_SERVERS))
                    .property(TOPIC, source.getStringProperty(TOPIC))
                    .property(PROTOCOL, source.getStringProperty(PROTOCOL))
                    .property(MECHANISM, source.getStringProperty(MECHANISM))
                    .property(TOKEN, token)
                    .property(POLL_DURATION, pollDuration)
                    .property(GROUP_PREFIX, groupPrefix)
                    .build();

            var provisioned = ProvisionedResource.Builder.newInstance()
                    .id(resource.getId())
                    .flowId(flowId)
                    .dataAddress(edr)
                    .build();

            monitor.debug("Provisioned Kafka EDR for flow %s, topic %s".formatted(flowId, source.getStringProperty(TOPIC)));
            return completed(StatusResult.success(provisioned));
        } catch (Exception e) {
            return completed(StatusResult.failure(ResponseStatus.FATAL_ERROR, "Failed to provision Kafka data flow: " + e.getMessage()));
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

    private static CompletableFuture<StatusResult<ProvisionedResource>> completed(StatusResult<ProvisionedResource> result) {
        return CompletableFuture.completedFuture(result);
    }
}
