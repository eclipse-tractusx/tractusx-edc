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
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.tractusx.edc.dataplane.kafka.acl.KafkaAclService;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.dataplane.kafka.dataaddress.KafkaBrokerDataAddressSchema.GROUP_PREFIX;
import static org.eclipse.tractusx.edc.dataplane.kafka.dataaddress.KafkaBrokerDataAddressSchema.KAFKA_TYPE;
import static org.eclipse.tractusx.edc.dataplane.kafka.dataaddress.KafkaBrokerDataAddressSchema.TOKEN;
import static org.eclipse.tractusx.edc.dataplane.kafka.dataaddress.KafkaBrokerDataAddressSchema.TOPIC;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class KafkaEndpointDataReferenceServiceTest {

    private static final String FLOW_ID = "flow-1";
    private static final String GROUP = "group-prefix";

    private final KafkaAclService aclService = mock();
    private final KafkaEndpointDataReferenceService service =
            new KafkaEndpointDataReferenceService(aclService, mock(Monitor.class), new ObjectMapper());

    @Test
    void createEndpointDataReference_createsAcls_andReturnsEdr() {
        var edr = edr(jwtWithSub("kafka-subject"), "test-topic", GROUP);
        var dataFlow = dataFlow(edr);
        when(aclService.createAclsForSubject(any(), any(), any(), any())).thenReturn(Result.success());

        var result = service.createEndpointDataReference(dataFlow);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).isSameAs(edr);
        verify(aclService).createAclsForSubject("kafka-subject", "test-topic", GROUP, FLOW_ID);
    }

    @Test
    void createEndpointDataReference_fails_whenNoActualSource() {
        var dataFlow = mock(DataFlow.class);
        when(dataFlow.getId()).thenReturn(FLOW_ID);
        when(dataFlow.getActualSource()).thenReturn(null);

        assertThat(service.createEndpointDataReference(dataFlow).failed()).isTrue();
    }

    @Test
    void createEndpointDataReference_fails_whenAclCreationFails() {
        var dataFlow = dataFlow(edr(jwtWithSub("kafka-subject"), "test-topic", GROUP));
        when(aclService.createAclsForSubject(any(), any(), any(), any())).thenReturn(Result.failure("broker error"));

        assertThat(service.createEndpointDataReference(dataFlow).failed()).isTrue();
    }

    @Test
    void createEndpointDataReference_returnsEdr_whenAclManagementDisabled() {
        var noAcl = new KafkaEndpointDataReferenceService(null, mock(Monitor.class), new ObjectMapper());
        var edr = edr("any-token", "test-topic", GROUP);

        var result = noAcl.createEndpointDataReference(dataFlow(edr));

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).isSameAs(edr);
        verifyNoInteractions(aclService);
    }

    @Test
    void revoke_revokesAcls_onSuspendOrTerminate() {
        when(aclService.revokeAclsForTransferProcess(FLOW_ID)).thenReturn(Result.success());

        var result = service.revokeEndpointDataReference(FLOW_ID, "suspended");

        assertThat(result.succeeded()).isTrue();
        verify(aclService).revokeAclsForTransferProcess(FLOW_ID);
    }

    @Test
    void revoke_fails_whenAclRevocationFails() {
        when(aclService.revokeAclsForTransferProcess(FLOW_ID)).thenReturn(Result.failure("broker error"));

        assertThat(service.revokeEndpointDataReference(FLOW_ID, "terminated").failed()).isTrue();
    }

    @Test
    void revoke_isNoOp_whenAclManagementDisabled() {
        var noAcl = new KafkaEndpointDataReferenceService(null, mock(Monitor.class), new ObjectMapper());

        var result = noAcl.revokeEndpointDataReference(FLOW_ID, "suspended");

        assertThat(result.succeeded()).isTrue();
        verifyNoInteractions(aclService);
    }

    private static DataFlow dataFlow(DataAddress actualSource) {
        var dataFlow = mock(DataFlow.class);
        when(dataFlow.getId()).thenReturn(FLOW_ID);
        when(dataFlow.getActualSource()).thenReturn(actualSource);
        return dataFlow;
    }

    private static DataAddress edr(String token, String topic, String groupPrefix) {
        return DataAddress.Builder.newInstance()
                .type(KAFKA_TYPE)
                .property(TOKEN, token)
                .property(TOPIC, topic)
                .property(GROUP_PREFIX, groupPrefix)
                .build();
    }

    private static String jwtWithSub(String sub) {
        var enc = Base64.getUrlEncoder().withoutPadding();
        var header = enc.encodeToString("{\"alg\":\"none\"}".getBytes());
        var payload = enc.encodeToString(("{\"sub\":\"" + sub + "\"}").getBytes());
        var sig = enc.encodeToString("sig".getBytes());
        return header + "." + payload + "." + sig;
    }
}
