/********************************************************************************
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
 ********************************************************************************/

package org.eclipse.tractusx.edc.tests.bpn.events;

import jakarta.json.Json;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.tractusx.edc.tests.participant.TransferParticipant;
import org.eclipse.tractusx.edc.tests.runtimes.PostgresExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_BPN;
import static org.eclipse.tractusx.edc.tests.TestRuntimeConfiguration.PROVIDER_NAME;
import static org.eclipse.tractusx.edc.tests.runtimes.Runtimes.pgRuntime;

@EndToEndTest
public class BusinessPartnerEventTest {
    private static final TransferParticipant PROVIDER = TransferParticipant.Builder.newInstance()
            .name(PROVIDER_NAME)
            .id(PROVIDER_BPN)
            .enableEventSubscription()
            .build();

    private static final List<String> GROUPS = List.of("group1", "group2");

    @RegisterExtension
    @Order(0)
    private static final PostgresExtension POSTGRES = new PostgresExtension(PROVIDER.getName());

    @RegisterExtension
    private static final RuntimeExtension PROVIDER_RUNTIME = pgRuntime(PROVIDER, POSTGRES);

    @BeforeEach
    void setUp() {
        PROVIDER.storeBusinessPartner(PROVIDER.getBpn(), GROUPS.toArray(new String[0]));
    }

    @DisplayName("BusinessPartnerCreated event is published")
    @Test
    void shouldPublishBusinessPartnerCreatedEvent() {
        var event = PROVIDER.waitForEvent("BusinessPartnerCreated");
        var payload = event.getJsonObject("payload");

        assertThat(payload).containsKeys("bpn", "groups");
        assertThat(payload.getString("bpn")).isEqualTo(PROVIDER.getBpn());
        assertThat(payload.getJsonArray("groups")).containsExactlyElementsOf(
                GROUPS.stream()
                        .map(Json::createValue)
                        .toList()
        );
    }

    @DisplayName("BusinessPartnerUpdated event is published")
    @Test
    void shouldPublishBusinessPartnerUpdatedEvent() {
        var updatedGroups = List.of("group1", "group3", "group4");

        PROVIDER.updateBusinessPartner(PROVIDER.getBpn(), updatedGroups.toArray(new String[0]));
        var event = PROVIDER.waitForEvent("BusinessPartnerUpdated");
        var payload = event.getJsonObject("payload");

        assertThat(payload).containsKeys("bpn", "groups");
        assertThat(payload.getString("bpn")).isEqualTo(PROVIDER.getBpn());
        assertThat(payload.getJsonArray("groups")).containsExactlyElementsOf(
                updatedGroups.stream()
                        .map(Json::createValue)
                        .toList()
        );
    }

    @DisplayName("BusinessPartnerDeleted event is published")
    @Test
    void shouldPublishBusinessPartnerDeletedEvent() {
        PROVIDER.deleteBusinessPartner(PROVIDER.getBpn());
        var event = PROVIDER.waitForEvent("BusinessPartnerDeleted");
        var payload = event.getJsonObject("payload");

        assertThat(payload).containsKey("bpn");
        assertThat(payload.getString("bpn")).isEqualTo(PROVIDER.getBpn());
    }
}
