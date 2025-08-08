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
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.tractusx.edc.agreements.bpns.spi.store.AgreementsBpnsStore;
import org.eclipse.tractusx.edc.agreements.bpns.spi.types.AgreementsBpnsEntry;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;

public class EventContractNegotiationSubscriber implements EventSubscriber {
    private final AgreementsBpnsStore store;
    private final Monitor monitor;
    private final BdrsClient bdrsClient;
    private static final String DID_PREFIX = "did";

    public EventContractNegotiationSubscriber(AgreementsBpnsStore store, Monitor monitor, BdrsClient bdrsClient) {
        this.store = store;
        this.monitor = monitor;
        this.bdrsClient = bdrsClient;
    }


    @Override
    public <E extends Event> void on(EventEnvelope<E> envelope) {
        var payload = (ContractNegotiationFinalized) envelope.getPayload();
        var agreement = payload.getContractAgreement();
        var agreementId = agreement.getId();

        var providerBpn = extractBpn(agreement.getProviderId());
        var consumerBpn = extractBpn(agreement.getConsumerId());

        var entry = AgreementsBpnsEntry.Builder.newInstance()
                .withAgreementId(agreementId)
                .withProviderBpn(providerBpn)
                .withConsumerBpn(consumerBpn)
                .build();

        store.save(entry).onFailure(failure -> monitor.severe(failure.getFailureDetail()));
    }

    private String extractBpn(String id) {
        return id.startsWith(DID_PREFIX)
                ? bdrsClient.resolveBpn(id)
                : id;
    }
}
