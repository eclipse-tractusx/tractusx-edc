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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class BusinessPartnerEventListenerTest {

    private Clock clock;
    private EventRouter eventRouter;
    private BusinessPartnerEventListener listener;

    private static final String BPN = "test-bpn";
    private static final List<String> GROUPS = List.of("group1", "group2");

    @BeforeEach
    void setUp() {
        clock = mock(Clock.class);
        eventRouter = mock(EventRouter.class);
        listener = new BusinessPartnerEventListener(clock, eventRouter);

        when(clock.millis()).thenReturn(123456789L);
    }

    @Test
    void testCreatedEvent() {
        listener.created(BPN, GROUPS);

        ArgumentCaptor<EventEnvelope> captor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(eventRouter).publish(captor.capture());

        var envelope = captor.getValue();
        var event = envelope.getPayload();

        assertEquals(BusinessPartnerCreated.class, event.getClass());
        assertEquals("bpn.created", event.name());
        assertEquals(BPN, ((BusinessPartnerCreated) event).getBpn());
        assertEquals(GROUPS, ((BusinessPartnerCreated) event).getGroups());
        assertEquals(123456789L, envelope.getAt());
    }

    @Test
    void testDeletedEvent() {
        listener.deleted(BPN);

        ArgumentCaptor<EventEnvelope> captor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(eventRouter).publish(captor.capture());

        var envelope = captor.getValue();
        var event = envelope.getPayload();

        assertEquals(BusinessPartnerDeleted.class, event.getClass());
        assertEquals("bpn.deleted", event.name());
        assertEquals(BPN, ((BusinessPartnerDeleted) event).getBpn());
        assertEquals(123456789L, envelope.getAt());
    }

    @Test
    void testUpdatedEvent() {
        listener.updated(BPN, GROUPS);

        ArgumentCaptor<EventEnvelope> captor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(eventRouter).publish(captor.capture());

        var envelope = captor.getValue();
        var event = envelope.getPayload();

        assertEquals(BusinessPartnerUpdated.class, event.getClass());
        assertEquals("bpn.updated", event.name());
        assertEquals(BPN, ((BusinessPartnerUpdated) event).getBpn());
        assertEquals(GROUPS, ((BusinessPartnerUpdated) event).getGroups());
        assertEquals(123456789L, envelope.getAt());
    }
}
