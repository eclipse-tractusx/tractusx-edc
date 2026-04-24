/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
 ********************************************************************************/

package org.eclipse.tractusx.edc.flow;

import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.tractusx.edc.agreements.bpns.spi.store.AgreementsBpnsStore;
import org.eclipse.tractusx.edc.agreements.bpns.spi.types.AgreementsBpnsEntry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.AUDIENCE_PROPERTY;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.BPN_PROPERTY;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TxDataFlowPropertiesProviderTest {

    private static final String CONTRACT_ID = "test-contract-id";
    private static final String CONSUMER_DID = "did:web:consumer";
    private static final String CONSUMER_BPN = "BPNL000000000001";

    private final AgreementsBpnsStore store = mock();
    private final TxDataFlowPropertiesProvider provider = new TxDataFlowPropertiesProvider(store);

    @Test
    void shouldReturnProperties_whenEntryFound() {
        var entry = AgreementsBpnsEntry.Builder.newInstance()
                .withAgreementId(CONTRACT_ID)
                .withProviderBpn("BPNL000000000002")
                .withConsumerBpn(CONSUMER_BPN)
                .build();
        when(store.findByAgreementId(CONTRACT_ID)).thenReturn(entry);

        var result = provider.propertiesFor(createTransferProcess(CONTRACT_ID), createPolicy(CONSUMER_DID));

        assertThat(result).isSucceeded().satisfies(properties -> {
            assertThat(properties).containsEntry(AUDIENCE_PROPERTY, CONSUMER_DID);
            assertThat(properties).containsEntry(BPN_PROPERTY, CONSUMER_BPN);
        });
    }

    @Test
    void shouldReturnFatalError_whenEntryNotFound() {
        when(store.findByAgreementId(CONTRACT_ID)).thenReturn(null);

        var result = provider.propertiesFor(createTransferProcess(CONTRACT_ID), createPolicy(CONSUMER_DID));

        assertThat(result).isFailed()
                .detail().isEqualTo("No BPN entry found for agreement %s".formatted(CONTRACT_ID));
    }

    private TransferProcess createTransferProcess(String contractId) {
        return TransferProcess.Builder.newInstance().contractId(contractId).build();
    }

    private Policy createPolicy(String assignee) {
        return Policy.Builder.newInstance().assignee(assignee).build();
    }
}
