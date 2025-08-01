/*
 * Copyright (c) 2025 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.api.bpn;

import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.tractusx.edc.validation.businesspartner.spi.event.BusinessPartnerCreated;
import org.eclipse.tractusx.edc.validation.businesspartner.spi.event.BusinessPartnerDeleted;
import org.eclipse.tractusx.edc.validation.businesspartner.spi.event.BusinessPartnerUpdated;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BusinessPartnerEventListenerTest {

    private final Clock clock = mock();
    private final EventRouter eventRouter = mock();
    private final BusinessPartnerEventListener listener = new BusinessPartnerEventListener(clock, eventRouter);

    private static final String BPN = "test-bpn";
    private static final List<String> GROUPS = List.of("group1", "group2");
    private static final Long TIMESTAMP = 123456789L;

    @BeforeEach
    void setUp() {
        when(clock.millis()).thenReturn(TIMESTAMP);
    }

    @Test
    void testCreatedEvent() {
        listener.created(BPN, GROUPS);

        ArgumentCaptor<EventEnvelope> captor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(eventRouter).publish(captor.capture());

        var envelope = captor.getValue();
        var event = envelope.getPayload();

        assertThat(event).isInstanceOf(BusinessPartnerCreated.class);
        assertThat(event.name()).isEqualTo("bpn.created");
        assertThat(((BusinessPartnerCreated) event).getBpn())
                .isEqualTo(BPN);
        assertThat(((BusinessPartnerCreated) event).getGroups())
                .isEqualTo(GROUPS);
        assertThat(envelope.getAt()).isEqualTo(TIMESTAMP);
    }

    @Test
    void testDeletedEvent() {
        listener.deleted(BPN);

        ArgumentCaptor<EventEnvelope> captor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(eventRouter).publish(captor.capture());

        var envelope = captor.getValue();
        var event = envelope.getPayload();

        assertThat(event).isInstanceOf(BusinessPartnerDeleted.class);
        assertThat(event.name()).isEqualTo("bpn.deleted");
        assertThat(((BusinessPartnerDeleted) event).getBpn())
                .isEqualTo(BPN);
        assertThat(envelope.getAt()).isEqualTo(TIMESTAMP);
    }

    @Test
    void testUpdatedEvent() {
        listener.updated(BPN, GROUPS);

        ArgumentCaptor<EventEnvelope> captor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(eventRouter).publish(captor.capture());

        var envelope = captor.getValue();
        var event = envelope.getPayload();

        assertThat(event).isInstanceOf(BusinessPartnerUpdated.class);
        assertThat(event.name()).isEqualTo("bpn.updated");
        assertThat(((BusinessPartnerUpdated) event).getBpn())
                .isEqualTo(BPN);
        assertThat(((BusinessPartnerUpdated) event).getGroups())
                .isEqualTo(GROUPS);
        assertThat(envelope.getAt()).isEqualTo(TIMESTAMP);
    }
}
