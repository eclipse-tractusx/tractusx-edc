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

/**
 * Constants shared by the Kafka data-plane provisioning components.
 */
public interface KafkaProvisionConstants {

    /**
     * Provision-resource type for Kafka flows. Matches the {@code KafkaBroker} source DataAddress type so
     * the data plane advertises {@code KafkaBroker} as an allowed source type and {@code KafkaBroker-PULL}
     * as an allowed transfer type.
     */
    String KAFKA_RESOURCE_TYPE = "KafkaBroker";

    /**
     * ProvisionResource property carrying the consumer participant id, used as the consumer-group prefix
     * fallback when the {@code kafka.group.prefix} DataAddress property is absent.
     */
    String CONSUMER_GROUP_PREFIX_PROPERTY = "tx:kafka:consumerGroupPrefix";

    /**
     * Default consumer poll duration (ISO-8601) when {@code kafka.poll.duration} is not set.
     */
    String DEFAULT_POLL_DURATION = "PT1S";
}
