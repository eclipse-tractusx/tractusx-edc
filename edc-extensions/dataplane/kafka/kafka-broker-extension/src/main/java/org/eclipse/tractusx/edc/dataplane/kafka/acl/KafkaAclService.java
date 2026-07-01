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

package org.eclipse.tractusx.edc.dataplane.kafka.acl;

import org.eclipse.edc.spi.result.Result;

/**
 * Manages Kafka ACLs (Access Control Lists) for transfer processes.
 * <p>
 * When ACL management is enabled, each started transfer creates topic-scoped
 * READ/DESCRIBE ACLs for the consumer's OAuth subject, and those ACLs are
 * revoked immediately on suspend or terminate — providing broker-level
 * enforcement that is independent of OAuth token expiry.
 */
public interface KafkaAclService {

    /**
     * Creates READ and DESCRIBE ACLs on the given topic plus a READ ACL on the consumer group.
     * <p>
     * The ACL principal is always {@code User:<oauthSubject>} (the broker identity carried in the
     * consumer's OAUTHBEARER token). The consumer-group resource is named by {@code groupPrefix}
     * (PREFIXED), which must be the same group prefix that is handed to the consumer in the EDR so the
     * broker grant and the consumer instruction cannot diverge.
     *
     * @param oauthSubject      the OAuth {@code sub} claim extracted from the consumer's JWT (the principal)
     * @param topicName         the Kafka topic to grant access to
     * @param groupPrefix       the consumer-group prefix to grant READ on (PREFIXED match)
     * @param transferProcessId used to track the ACLs for later revocation
     */
    Result<Void> createAclsForSubject(String oauthSubject, String topicName, String groupPrefix, String transferProcessId);

    /**
     * Revokes all ACLs that were created for the given transfer process.
     */
    Result<Void> revokeAclsForTransferProcess(String transferProcessId);

    /**
     * Revokes ACLs for the given subject, topic and group prefix directly, without requiring a transfer
     * process ID. The arguments must match those used at {@link #createAclsForSubject} for the deletion
     * filters to match the created bindings.
     */
    Result<Void> revokeAclsForSubject(String oauthSubject, String topicName, String groupPrefix);
}
