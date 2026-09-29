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

import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionResource;
import org.eclipse.edc.connector.dataplane.spi.provision.ResourceDefinitionGenerator;

import java.util.UUID;

import static org.eclipse.tractusx.edc.dataplane.kafka.provision.KafkaProvisionConstants.CONSUMER_GROUP_PREFIX_PROPERTY;
import static org.eclipse.tractusx.edc.dataplane.kafka.provision.KafkaProvisionConstants.KAFKA_RESOURCE_TYPE;

/**
 * Provider-side {@link ResourceDefinitionGenerator} for the {@code KafkaBroker} source type. Registering it
 * makes the data plane advertise {@code KafkaBroker} as an allowed source type (and, via the EDR service,
 * {@code KafkaBroker-PULL} as a transfer type) and triggers {@link KafkaProvisioner} on transfer start.
 */
public class KafkaResourceDefinitionGenerator implements ResourceDefinitionGenerator {

    @Override
    public String supportedType() {
        return KAFKA_RESOURCE_TYPE;
    }

    @Override
    public ProvisionResource generate(DataFlow dataFlow) {
        return ProvisionResource.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .flowId(dataFlow.getId())
                .type(KAFKA_RESOURCE_TYPE)
                .dataAddress(dataFlow.getSource())
                .property(CONSUMER_GROUP_PREFIX_PROPERTY, dataFlow.getParticipantId())
                .build();
    }
}
