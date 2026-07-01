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

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.CreateAclsResult;
import org.apache.kafka.clients.admin.DeleteAclsResult;
import org.apache.kafka.common.acl.AccessControlEntry;
import org.apache.kafka.common.acl.AccessControlEntryFilter;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclBindingFilter;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourcePatternFilter;
import org.apache.kafka.common.resource.ResourceType;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class KafkaAclServiceImpl implements KafkaAclService {

    private final Properties kafkaProperties;
    private final Monitor monitor;
    private final AdminClientFactory adminClientFactory;
    private final Map<String, AclTrackingInfo> transferProcessAcls = new ConcurrentHashMap<>();

    public KafkaAclServiceImpl(Properties kafkaProperties, Monitor monitor) {
        this(kafkaProperties, monitor, new DefaultAdminClientFactory());
    }

    public KafkaAclServiceImpl(Properties kafkaProperties, Monitor monitor, AdminClientFactory adminClientFactory) {
        this.kafkaProperties = kafkaProperties;
        this.monitor = monitor;
        this.adminClientFactory = adminClientFactory;
    }

    @Override
    public Result<Void> createAclsForSubject(String oauthSubject, String topicName, String groupPrefix, String transferProcessId) {
        monitor.debug("Creating ACLs for OAuth subject: %s, topic: %s, groupPrefix: %s, transferProcessId: %s"
                .formatted(oauthSubject, topicName, groupPrefix, transferProcessId));

        try (Admin adminClient = adminClientFactory.createAdmin(kafkaProperties)) {
            Collection<AclBinding> aclBindings = buildAclBindings(oauthSubject, topicName, groupPrefix);
            CreateAclsResult result = adminClient.createAcls(aclBindings);
            result.all().get();

            transferProcessAcls.put(transferProcessId, new AclTrackingInfo(oauthSubject, topicName, aclBindings));

            monitor.debug("Successfully created ACLs for OAuth subject: %s, topic: %s, transferProcessId: %s"
                    .formatted(oauthSubject, topicName, transferProcessId));
            return Result.success();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String message = "Interrupted while creating ACLs for subject: %s".formatted(oauthSubject);
            monitor.severe(message, e);
            return failure(message, e);
        } catch (ExecutionException e) {
            String message = "Failed to create ACLs for OAuth subject: %s".formatted(oauthSubject);
            monitor.severe(message, e);
            return failure(message, e);
        }
    }

    @Override
    public Result<Void> revokeAclsForTransferProcess(String transferProcessId) {
        monitor.debug("Revoking ACLs for transferProcessId: %s".formatted(transferProcessId));

        AclTrackingInfo aclInfo = transferProcessAcls.remove(transferProcessId);
        if (aclInfo == null) {
            monitor.debug("No ACLs found for transferProcessId: %s".formatted(transferProcessId));
            return Result.success();
        }

        try (Admin adminClient = adminClientFactory.createAdmin(kafkaProperties)) {
            Collection<AclBindingFilter> aclFilters = toAclBindingFilters(aclInfo.aclBindings());
            DeleteAclsResult result = adminClient.deleteAcls(aclFilters);
            result.all().get();

            monitor.debug("Successfully revoked ACLs for transferProcessId: %s".formatted(transferProcessId));
            return Result.success();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String message = "Interrupted while revoking ACLs for transferProcessId: %s".formatted(transferProcessId);
            monitor.severe(message, e);
            return failure(message, e);
        } catch (ExecutionException e) {
            String message = "Failed to revoke ACLs for transferProcessId: %s".formatted(transferProcessId);
            monitor.severe(message, e);
            return failure(message, e);
        }
    }

    @Override
    public Result<Void> revokeAclsForSubject(String oauthSubject, String topicName, String groupPrefix) {
        monitor.debug("Revoking ACLs for OAuth subject: %s, topic: %s, groupPrefix: %s".formatted(oauthSubject, topicName, groupPrefix));

        try (Admin adminClient = adminClientFactory.createAdmin(kafkaProperties)) {
            Collection<AclBindingFilter> aclFilters = toAclBindingFilters(buildAclBindings(oauthSubject, topicName, groupPrefix));
            DeleteAclsResult result = adminClient.deleteAcls(aclFilters);
            result.all().get();

            monitor.debug("Successfully revoked ACLs for OAuth subject: %s, topic: %s".formatted(oauthSubject, topicName));
            return Result.success();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String message = "Interrupted while revoking ACLs for subject: %s".formatted(oauthSubject);
            monitor.severe(message, e);
            return failure(message, e);
        } catch (ExecutionException e) {
            String message = "Failed to revoke ACLs for OAuth subject: %s".formatted(oauthSubject);
            monitor.severe(message, e);
            return failure(message, e);
        }
    }

    private @NotNull Result<Void> failure(String message, Exception e) {
        return Result.failure("%s: %s".formatted(message, e.getMessage()));
    }

    private Collection<AclBinding> buildAclBindings(String oauthSubject, String topicName, String groupPrefix) {
        String principal = "User:" + oauthSubject;
        ResourcePattern topicResource = new ResourcePattern(ResourceType.TOPIC, topicName, PatternType.LITERAL);
        ResourcePattern groupResource = new ResourcePattern(ResourceType.GROUP, groupPrefix, PatternType.PREFIXED);

        return List.of(
                new AclBinding(topicResource, new AccessControlEntry(principal, "*", AclOperation.READ, AclPermissionType.ALLOW)),
                new AclBinding(topicResource, new AccessControlEntry(principal, "*", AclOperation.DESCRIBE, AclPermissionType.ALLOW)),
                new AclBinding(groupResource, new AccessControlEntry(principal, "*", AclOperation.READ, AclPermissionType.ALLOW))
        );
    }

    private Collection<AclBindingFilter> toAclBindingFilters(Collection<AclBinding> aclBindings) {
        Collection<AclBindingFilter> filters = new ArrayList<>();
        for (AclBinding b : aclBindings) {
            filters.add(new AclBindingFilter(
                    new ResourcePatternFilter(b.pattern().resourceType(), b.pattern().name(), b.pattern().patternType()),
                    new AccessControlEntryFilter(b.entry().principal(), b.entry().host(), b.entry().operation(), b.entry().permissionType())
            ));
        }
        return filters;
    }

    private record AclTrackingInfo(String oauthSubject, String topicName, Collection<AclBinding> aclBindings) {
    }
}
