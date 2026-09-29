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

package org.eclipse.tractusx.edc.dataplane.kafka.flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.edr.EndpointDataReferenceService;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.tractusx.edc.dataplane.kafka.acl.KafkaAclService;
import org.jetbrains.annotations.Nullable;

import java.util.Base64;

import static org.eclipse.tractusx.edc.dataplane.kafka.dataaddress.KafkaBrokerDataAddressSchema.GROUP_PREFIX;
import static org.eclipse.tractusx.edc.dataplane.kafka.dataaddress.KafkaBrokerDataAddressSchema.TOKEN;
import static org.eclipse.tractusx.edc.dataplane.kafka.dataaddress.KafkaBrokerDataAddressSchema.TOPIC;

/**
 * Produces the consumer-facing EDR for a {@code KafkaBroker} PULL flow, and owns the per-activation broker
 * ACL lifecycle.
 * <p>
 * {@link #createEndpointDataReference} runs on transfer start <em>and</em> resume, so it (re)creates the
 * broker ACLs each time — restoring access on resume after a suspend revoked them. The EDR itself is the
 * provisioned {@link DataAddress} ({@link DataFlow#getActualSource()}: broker coordinates, topic, security
 * settings, consumer-group prefix and the minted token) built by
 * {@link org.eclipse.tractusx.edc.dataplane.kafka.provision.KafkaProvisioner}.
 * <p>
 * {@link #revokeEndpointDataReference} runs on suspend <em>and</em> terminate, revoking the ACLs so suspend
 * cuts access immediately; the OAuth token is revoked by the deprovisioner on terminate (where the client
 * credentials are available). When ACL management is disabled both are no-ops and access ends at the
 * token's TTL.
 */
public class KafkaEndpointDataReferenceService implements EndpointDataReferenceService {

    @Nullable
    private final KafkaAclService aclService;
    private final Monitor monitor;
    private final ObjectMapper objectMapper;

    public KafkaEndpointDataReferenceService(@Nullable KafkaAclService aclService, Monitor monitor, ObjectMapper objectMapper) {
        this.aclService = aclService;
        this.monitor = monitor;
        this.objectMapper = objectMapper;
    }

    @Override
    public Result<DataAddress> createEndpointDataReference(DataFlow dataFlow) {
        var edr = dataFlow.getActualSource();
        if (edr == null) {
            return Result.failure("No provisioned Kafka EDR available for data flow " + dataFlow.getId());
        }

        if (aclService != null) {
            try {
                var subject = extractOauthSubject(edr.getStringProperty(TOKEN));
                var aclResult = aclService.createAclsForSubject(subject, edr.getStringProperty(TOPIC),
                        edr.getStringProperty(GROUP_PREFIX), dataFlow.getId());
                if (aclResult.failed()) {
                    return Result.failure("Failed to create Kafka ACLs: " + aclResult.getFailureDetail());
                }
            } catch (EdcException e) {
                return Result.failure(e.getMessage());
            }
        }

        return Result.success(edr);
    }

    @Override
    public ServiceResult<Void> revokeEndpointDataReference(String transferProcessId, String reason) {
        if (aclService == null) {
            return ServiceResult.success();
        }
        monitor.debug("Revoking Kafka ACLs for data flow %s".formatted(transferProcessId));
        return ServiceResult.from(aclService.revokeAclsForTransferProcess(transferProcessId));
    }

    private String extractOauthSubject(String token) {
        try {
            var parts = token.split("\\.");
            if (parts.length != 3) {
                throw new EdcException("Invalid JWT token format");
            }
            var payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            var subNode = objectMapper.readTree(payload).get("sub");
            if (subNode == null || subNode.isNull()) {
                throw new EdcException("No 'sub' claim found in JWT token");
            }
            return subNode.asText();
        } catch (EdcException e) {
            throw e;
        } catch (Exception e) {
            throw new EdcException("Failed to extract OAuth subject from token: " + e.getMessage(), e);
        }
    }
}
