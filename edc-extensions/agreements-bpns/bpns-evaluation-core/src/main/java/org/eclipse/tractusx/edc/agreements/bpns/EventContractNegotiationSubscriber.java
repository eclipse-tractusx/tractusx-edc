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
import org.eclipse.tractusx.edc.core.utils.credentials.MembershipCredentialUtils;

public class EventContractNegotiationSubscriber implements EventSubscriber {
    private final AgreementsBpnsStore store;
    private final Monitor monitor;
    private final String ownBpn;

    public EventContractNegotiationSubscriber(AgreementsBpnsStore store, Monitor monitor, String ownBpn) {
        this.store = store;
        this.monitor = monitor.withPrefix(getClass().getSimpleName());
        this.ownBpn = ownBpn;
    }

    @Override
    public <E extends Event> void on(EventEnvelope<E> envelope) {
        var payload = (ContractNegotiationFinalized) envelope.getPayload();
        var agreement = payload.getContractAgreement();
        var agreementId = agreement.getId();

        var providerBpn = extractBpn(agreement.getProviderId(), agreement.getClaims(), true);
        var consumerBpn = extractBpn(agreement.getConsumerId(), agreement.getClaims(), false);

        if (providerBpn == null || consumerBpn == null) {
            monitor.debug("Could not create BPN mapping for agreement '%s': providerBpn=%s, consumerBpn=%s"
                    .formatted(agreementId, providerBpn, consumerBpn));
            return;
        }

        var entry = AgreementsBpnsEntry.Builder.newInstance()
                .withAgreementId(agreementId)
                .withProviderBpn(providerBpn)
                .withConsumerBpn(consumerBpn)
                .build();

        store.save(entry).onFailure(failure -> monitor.severe(failure.getFailureDetail()));
    }

    private String extractBpn(String id, java.util.Map<String, Object> claims, boolean ownParticipant) {
        if (!id.startsWith("did")) {
            return id;
        }
        var counterpartyBpn = MembershipCredentialUtils.extractBpn(claims, id);
        if (counterpartyBpn.isPresent()) {
            return counterpartyBpn.get();
        }
        if (ownParticipant) {
            return ownBpn;
        }
        return null;
    }
}
