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

package org.eclipse.tractusx.edc.dataplane.kafka.dataaddress;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Defines the schema of a DataAddress representing a Kafka endpoint.
 */
public interface KafkaBrokerDataAddressSchema {

    /**
     * The transfer type.
     */
    String KAFKA_TYPE = "KafkaBroker";

    /**
     * The Kafka topic that will be allowed to poll for the consumer.
     */
    String TOPIC = EDC_NAMESPACE + "topic";

    /**
     * The kafka.bootstrap.servers property.
     */
    String BOOTSTRAP_SERVERS = EDC_NAMESPACE + "kafka.bootstrap.servers";

    /**
     * The kafka.poll.duration property which specifies the duration of the consumer polling.
     * <p>
     * The value should be a ISO-8601 duration e.g. "PT10S" for 10 seconds.
     * This parameter is optional. The default value is 1 second.
     *
     * @see java.time.Duration#parse(CharSequence) for ISO-8601 duration format
     */
    String POLL_DURATION = EDC_NAMESPACE + "kafka.poll.duration";

    /**
     * The kafka.group.prefix that will be allowed to use for the consumer.
     */
    String GROUP_PREFIX = EDC_NAMESPACE + "kafka.group.prefix";

    /**
     * The security.protocol property.
     */
    String PROTOCOL = EDC_NAMESPACE + "kafka.security.protocol";

    /**
     * The sasl.mechanism property.
     */
    String MECHANISM = EDC_NAMESPACE + "kafka.sasl.mechanism";

    /**
     * The authentication token.
     */
    String TOKEN = EDC_NAMESPACE + "token";

    /**
     * The OAuth token URL for retrieving access tokens.
     */
    String OAUTH_TOKEN_URL = EDC_NAMESPACE + "tokenUrl";

    /**
     * The OAuth revoke URL for invalidating tokens.
     */
    String OAUTH_REVOKE_URL = EDC_NAMESPACE + "revokeUrl";

    /**
     * The OAuth client ID.
     */
    String OAUTH_CLIENT_ID = EDC_NAMESPACE + "clientId";

    /**
     * The OAuth client secret key.
     */
    String OAUTH_CLIENT_SECRET_KEY = EDC_NAMESPACE + "clientSecretKey";
}
