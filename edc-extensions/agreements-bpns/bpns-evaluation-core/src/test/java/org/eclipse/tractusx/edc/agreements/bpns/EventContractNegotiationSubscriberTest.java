/********************************************************************************
 * Copyright (c) 2025 Cofinity-X GmbH
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

package org.eclipse.tractusx.edc.agreements.bpns;


import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationFinalized;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.tractusx.edc.agreements.bpns.spi.store.AgreementsBpnsStore;
import org.eclipse.tractusx.edc.agreements.bpns.spi.types.AgreementsBpnsEntry;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;
import org.eclipse.tractusx.edc.tests.MockBdrsClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class EventContractNegotiationSubscriberTest {

    private final AgreementsBpnsStore store = mock();
    private final Monitor monitor = mock();
    private final BdrsClient bdrsClient = new MockBdrsClient((s) -> s, (s) -> "resolvedBpn");
    private EventContractNegotiationSubscriber subscriber;

    @BeforeEach
    void setup() {
        subscriber = new EventContractNegotiationSubscriber(store, monitor, bdrsClient);
    }

    @Test
    void on_shouldSaveEntryWithResolvedBpn_whenIdIsDid() {
        var agreementId = UUID.randomUUID().toString();
        var providerId = "did:provider";
        var consumerId = "did:consumer";
        var resolvedBpn = "resolvedBpn";

        when(store.save(any())).thenReturn(StoreResult.success());

        var agreement = ContractAgreement.Builder.newInstance()
                .id(agreementId)
                .providerId(providerId)
                .consumerId(consumerId)
                .assetId("asset")
                .policy(Policy.Builder.newInstance().assignee(consumerId).build())
                .build();

        var event = ContractNegotiationFinalized.Builder.newInstance()
                .contractNegotiationId(UUID.randomUUID().toString())
                .contractAgreement(agreement)
                .counterPartyAddress("counterPartyAddress")
                .counterPartyId("counterPartyId")
                .protocol("protocol")
                .build();

        var envelope = EventEnvelope.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .at(System.currentTimeMillis())
                .payload(event)
                .build();

        subscriber.on(envelope);

        ArgumentCaptor<AgreementsBpnsEntry> captor = ArgumentCaptor.forClass(AgreementsBpnsEntry.class);
        verify(store).save(captor.capture());
        AgreementsBpnsEntry entry = captor.getValue();
        assert entry.getAgreementId().equals(agreementId);
        assert entry.getProviderBpn().equals(resolvedBpn);
        assert entry.getConsumerBpn().equals(resolvedBpn);
    }

    @Test
    void on_shouldSaveEntryWithOriginalBpn_whenIdNotDid() {
        var agreementId = UUID.randomUUID().toString();
        var providerId = "providerBpn";
        var consumerId = "consumerBpn";

        when(store.save(any())).thenReturn(StoreResult.success());

        var agreement = ContractAgreement.Builder.newInstance()
                .id(agreementId)
                .providerId(providerId)
                .consumerId(consumerId)
                .assetId("asset")
                .policy(Policy.Builder.newInstance().assignee(consumerId).build())
                .build();

        var event = ContractNegotiationFinalized.Builder.newInstance()
                .contractNegotiationId(UUID.randomUUID().toString())
                .contractAgreement(agreement)
                .counterPartyAddress("counterPartyAddress")
                .counterPartyId("counterPartyId")
                .protocol("protocol")
                .build();

        var envelope = EventEnvelope.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .at(System.currentTimeMillis())
                .payload(event)
                .build();

        subscriber.on(envelope);

        ArgumentCaptor<AgreementsBpnsEntry> captor = ArgumentCaptor.forClass(AgreementsBpnsEntry.class);
        verify(store).save(captor.capture());
        AgreementsBpnsEntry entry = captor.getValue();
        assert entry.getProviderBpn().equals(providerId);
        assert entry.getConsumerBpn().equals(consumerId);
    }

    @Test
    void on_shouldLogSevere_whenStoreSaveFails() {
        var agreementId = UUID.randomUUID().toString();
        var providerId = "providerBpn";
        var consumerId = "consumerBpn";

        var failureDetail = "Contract Agreement is already exists.";
        when(store.save(any())).thenReturn(StoreResult.alreadyExists(failureDetail));

        var agreement = ContractAgreement.Builder.newInstance()
                .id(agreementId)
                .providerId(providerId)
                .consumerId(consumerId)
                .assetId("asset")
                .policy(Policy.Builder.newInstance().assignee(consumerId).build())
                .build();

        var event = ContractNegotiationFinalized.Builder.newInstance()
                .contractNegotiationId(UUID.randomUUID().toString())
                .contractAgreement(agreement)
                .counterPartyAddress("counterPartyAddress")
                .counterPartyId("counterPartyId")
                .protocol("protocol")
                .build();

        var envelope = EventEnvelope.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .at(System.currentTimeMillis())
                .payload(event)
                .build();

        subscriber.on(envelope);

        verify(monitor).severe(failureDetail);
    }
}
