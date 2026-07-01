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
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourceType;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class KafkaAclServiceImplTest {

    private static final String OAUTH_SUBJECT = "test-oauth-subject";
    private static final String TOPIC = "test-topic";
    // Distinct from the OAuth subject so the GROUP ACL is verified to be named by the group prefix
    // (PREFIXED) while the principal stays the subject.
    private static final String GROUP_PREFIX = "test-group-prefix";
    private static final String TRANSFER_PROCESS_ID = "test-transfer-process-id";

    private final Monitor monitor = mock();
    private final Admin mockAdmin = mock();
    private final AdminClientFactory adminClientFactory = mock();
    private KafkaAclServiceImpl aclService;

    @BeforeEach
    void setUp() {
        var kafkaProperties = new Properties();
        kafkaProperties.put("bootstrap.servers", "localhost:9092");
        when(adminClientFactory.createAdmin(any(Properties.class))).thenReturn(mockAdmin);
        aclService = new KafkaAclServiceImpl(kafkaProperties, monitor, adminClientFactory);
    }

    @Test
    void createAclsForSubject_shouldSucceed() throws ExecutionException, InterruptedException {
        setupSuccessfulCreate();

        Result<Void> result = aclService.createAclsForSubject(OAUTH_SUBJECT, TOPIC, GROUP_PREFIX, TRANSFER_PROCESS_ID);

        assertThat(result.succeeded()).isTrue();
        // 3 bindings: topic READ + DESCRIBE (LITERAL) and consumer-group READ (PREFIXED by the group
        // prefix, not the subject), all for principal User:<subject>.
        verify(mockAdmin).createAcls(argThat(bindings ->
                bindings.size() == 3 &&
                        hasBinding(bindings, ResourceType.TOPIC, TOPIC, PatternType.LITERAL, AclOperation.READ) &&
                        hasBinding(bindings, ResourceType.TOPIC, TOPIC, PatternType.LITERAL, AclOperation.DESCRIBE) &&
                        hasBinding(bindings, ResourceType.GROUP, GROUP_PREFIX, PatternType.PREFIXED, AclOperation.READ)));
    }

    private static boolean hasBinding(Collection<AclBinding> bindings, ResourceType resourceType, String name,
                                      PatternType patternType, AclOperation operation) {
        return bindings.stream().anyMatch(b ->
                b.pattern().resourceType() == resourceType &&
                        b.pattern().name().equals(name) &&
                        b.pattern().patternType() == patternType &&
                        b.entry().principal().equals("User:" + OAUTH_SUBJECT) &&
                        b.entry().operation() == operation);
    }

    @Test
    void createAclsForSubject_shouldFail_whenExecutionExceptionOccurs() throws ExecutionException, InterruptedException {
        CreateAclsResult createResult = mock(CreateAclsResult.class);
        KafkaFuture<Void> future = mock(KafkaFuture.class);
        when(mockAdmin.createAcls(anyCollection())).thenReturn(createResult);
        when(createResult.all()).thenReturn(future);
        when(future.get()).thenThrow(new ExecutionException("error", new RuntimeException()));

        Result<Void> result = aclService.createAclsForSubject(OAUTH_SUBJECT, TOPIC, GROUP_PREFIX, TRANSFER_PROCESS_ID);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureDetail()).contains("Failed to create ACLs for OAuth subject: " + OAUTH_SUBJECT);
    }

    @Test
    void revokeAclsForTransferProcess_shouldSucceed_afterCreate() throws Exception {
        setupSuccessfulCreate();
        aclService.createAclsForSubject(OAUTH_SUBJECT, TOPIC, GROUP_PREFIX, TRANSFER_PROCESS_ID);

        setupSuccessfulDelete();

        Result<Void> result = aclService.revokeAclsForTransferProcess(TRANSFER_PROCESS_ID);

        assertThat(result.succeeded()).isTrue();
        verify(mockAdmin).deleteAcls(anyCollection());
    }

    @Test
    void revokeAclsForTransferProcess_shouldSucceed_whenNoAclsTracked() {
        Result<Void> result = aclService.revokeAclsForTransferProcess("unknown-id");

        assertThat(result.succeeded()).isTrue();
        verifyNoInteractions(mockAdmin);
        verify(adminClientFactory, never()).createAdmin(any());
    }

    @Test
    void revokeAclsForTransferProcess_shouldFail_whenExecutionExceptionOccurs() throws Exception {
        setupSuccessfulCreate();
        aclService.createAclsForSubject(OAUTH_SUBJECT, TOPIC, GROUP_PREFIX, TRANSFER_PROCESS_ID);

        DeleteAclsResult deleteResult = mock(DeleteAclsResult.class);
        KafkaFuture<Collection<AclBinding>> future = mock(KafkaFuture.class);
        when(mockAdmin.deleteAcls(anyCollection())).thenReturn(deleteResult);
        when(deleteResult.all()).thenReturn(future);
        when(future.get()).thenThrow(new ExecutionException("error", new RuntimeException()));

        Result<Void> result = aclService.revokeAclsForTransferProcess(TRANSFER_PROCESS_ID);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureDetail()).contains("Failed to revoke ACLs for transferProcessId: " + TRANSFER_PROCESS_ID);
    }

    @Test
    void revokeAclsForSubject_shouldSucceed() throws Exception {
        setupSuccessfulDelete();

        Result<Void> result = aclService.revokeAclsForSubject(OAUTH_SUBJECT, TOPIC, GROUP_PREFIX);

        assertThat(result.succeeded()).isTrue();
        verify(mockAdmin).deleteAcls(anyCollection());
    }

    @Test
    void createAndRevoke_shouldTrackAndCleanUpAcls() throws Exception {
        setupSuccessfulCreate();
        setupSuccessfulDelete();

        aclService.createAclsForSubject(OAUTH_SUBJECT, TOPIC, GROUP_PREFIX, TRANSFER_PROCESS_ID);
        aclService.revokeAclsForTransferProcess(TRANSFER_PROCESS_ID);

        // After revoke, re-revoking for the same ID should be a no-op (no admin call)
        verify(adminClientFactory, times(2)).createAdmin(any());
        Result<Void> secondRevoke = aclService.revokeAclsForTransferProcess(TRANSFER_PROCESS_ID);
        assertThat(secondRevoke.succeeded()).isTrue();
        verify(adminClientFactory, times(2)).createAdmin(any()); // still 2, no extra call
    }

    private void setupSuccessfulCreate() throws ExecutionException, InterruptedException {
        CreateAclsResult createResult = mock(CreateAclsResult.class);
        KafkaFuture<Void> future = mock(KafkaFuture.class);
        when(mockAdmin.createAcls(anyCollection())).thenReturn(createResult);
        when(createResult.all()).thenReturn(future);
        when(future.get()).thenReturn(null);
    }

    private void setupSuccessfulDelete() throws ExecutionException, InterruptedException {
        DeleteAclsResult deleteResult = mock(DeleteAclsResult.class);
        KafkaFuture<Collection<AclBinding>> future = mock(KafkaFuture.class);
        when(mockAdmin.deleteAcls(anyCollection())).thenReturn(deleteResult);
        when(deleteResult.all()).thenReturn(future);
        when(future.get()).thenReturn(Collections.emptyList());
    }
}
