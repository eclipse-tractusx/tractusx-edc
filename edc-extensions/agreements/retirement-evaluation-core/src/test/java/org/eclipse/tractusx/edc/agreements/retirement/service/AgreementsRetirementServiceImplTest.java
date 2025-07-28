/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.agreements.retirement.service;

import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.services.spi.contractagreement.ContractAgreementService;
import org.eclipse.edc.spi.event.Event;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.tractusx.edc.agreements.retirement.spi.event.ContractAgreementReactivated;
import org.eclipse.tractusx.edc.agreements.retirement.spi.event.ContractAgreementRetired;
import org.eclipse.tractusx.edc.agreements.retirement.spi.service.AgreementsRetirementService;
import org.eclipse.tractusx.edc.agreements.retirement.spi.store.AgreementsRetirementStore;
import org.eclipse.tractusx.edc.agreements.retirement.spi.types.AgreementsRetirementEntry;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.CONFLICT;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgreementsRetirementServiceImplTest {

    private final AgreementsRetirementStore store = mock();
    private final TransactionContext transactionContext = new NoopTransactionContext();
    private final ContractAgreementService contractAgreementService = mock();
    private final EventRouter eventRouter = mock();

    private final AgreementsRetirementService service = new AgreementsRetirementServiceImpl(store, transactionContext,
            contractAgreementService, eventRouter, Clock.systemUTC());

    @Test
    void returnsTrue_ifAgreementIsRetired() {

        var agreementId = "test-agreement-id";

        var entry = AgreementsRetirementEntry.Builder.newInstance()
                .withAgreementId(agreementId)
                .withReason("mock-reason")
                .build();

        when(store.findRetiredAgreements(createFilterQueryByAgreementId(agreementId)))
                .thenReturn(Stream.of(entry));

        var result = service.isRetired(agreementId);

        assertThat(result).isTrue();
    }

    @Test
    void returnsFalse_ifAgreementIsNotRetired() {

        when(store.findRetiredAgreements(any(QuerySpec.class)))
                .thenReturn(Stream.of());

        var result = service.isRetired(anyString());

        assertThat(result).isFalse();
    }

    @Test
    void verify_findAll() {
        var query = QuerySpec.Builder.newInstance().build();
        when(store.findRetiredAgreements(query))
                .thenReturn(Stream.of());

        var result = service.findAll(query);
        assertThat(result).isSucceeded();
        assertThat(result.getContent()).hasSize(0);
    }

    @Nested
    class Retire {

        @Test
        void shouldRetireAgreement() {
            var contractAgreement = mock(ContractAgreement.class);
            when(contractAgreementService.findById(anyString())).thenReturn(contractAgreement);
            when(store.save(any())).thenReturn(StoreResult.success());

            var result = service.retireAgreement(createAgreementsRetirementEntry());

            assertThat(result).isSucceeded();
            verify(eventRouter).publish(ArgumentMatchers.<EventEnvelope<Event>>argThat(envelope -> envelope.getPayload().getClass().equals(ContractAgreementRetired.class)));
        }

        @Test
        void shouldReturnConflict_whenAgreementIsAlreadyRetired() {
            when(store.save(any())).thenReturn(StoreResult.alreadyExists("test"));
            var contractAgreement = mock(ContractAgreement.class);
            when(contractAgreementService.findById(anyString())).thenReturn(contractAgreement);

            var result = service.retireAgreement(createAgreementsRetirementEntry());

            assertThat(result).isFailed();
            assertThat(result.reason()).isEqualTo(CONFLICT);
        }

    }

    @Nested
    class Reactivate {

        @Test
        void shouldReactivateRetiredAgreement() {
            when(store.delete(any())).thenReturn(StoreResult.success());

            var result = service.reactivate(anyString());

            assertThat(result).isSucceeded();
            verify(eventRouter).publish(ArgumentMatchers.<EventEnvelope<Event>>argThat(envelope -> envelope.getPayload().getClass().equals(ContractAgreementReactivated.class)));
        }

        @Test
        void shouldReturnNotFound_whenAgreementHasNotBeenRetired() {
            when(store.delete(any()))
                    .thenReturn(StoreResult.notFound("test"));

            var result = service.reactivate(anyString());

            assertThat(result).isFailed();
            assertThat(result.reason()).isEqualTo(NOT_FOUND);
        }
    }

    private AgreementsRetirementEntry createAgreementsRetirementEntry() {
        return AgreementsRetirementEntry.Builder.newInstance()
                .withAgreementId(UUID.randomUUID().toString())
                .withReason("some-reason")
                .withAgreementRetirementDate(Instant.now().toEpochMilli())
                .build();
    }

    private QuerySpec createFilterQueryByAgreementId(String agreementId) {
        return QuerySpec.Builder.newInstance()
                .filter(
                        Criterion.Builder.newInstance()
                                .operandLeft("agreementId")
                                .operator("=")
                                .operandRight(agreementId)
                                .build()
                ).build();
    }

}
