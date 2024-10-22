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

import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.tractusx.edc.agreements.retirement.spi.service.AgreementsRetirementService;
import org.eclipse.tractusx.edc.agreements.retirement.spi.store.AgreementsRetirementStore;
import org.eclipse.tractusx.edc.agreements.retirement.spi.types.AgreementsRetirementEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.CONFLICT;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgreementsRetirementServiceImplTest {

    private AgreementsRetirementService service;
    private final AgreementsRetirementStore store = mock();
    private final TransactionContext transactionContext = new NoopTransactionContext();
    private final ContractNegotiationService contractNegotiationService = mock();

    @BeforeEach
    void setUp() {
        service = new AgreementsRetirementServiceImpl(store, transactionContext, contractNegotiationService);
    }

    @Test
    @DisplayName("Returns true if agreement is retired")
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
    @DisplayName("Returns false if agreement is not retired")
    void returnsFalse_ifAgreementIsNotRetired() {

        when(store.findRetiredAgreements(any(QuerySpec.class)))
                .thenReturn(Stream.of());

        var result = service.isRetired(anyString());

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Verify find all response")
    void verify_findAll() {
        var query = QuerySpec.Builder.newInstance().build();
        when(store.findRetiredAgreements(query))
                .thenReturn(Stream.of());

        var result = service.findAll(query);
        assertThat(result).isSucceeded();
        assertThat(result.getContent()).hasSize(0);
    }

    @Test
    @DisplayName("Verify reactivate response on failure")
    void verify_reactivateResponseOnFailure() {
        when(store.delete(any()))
                .thenReturn(StoreResult.notFound("test"));

        var result = service.reactivate(anyString());
        assertThat(result).isFailed();
        assertThat(result.reason()).isEqualTo(NOT_FOUND);
    }

    @Test
    @DisplayName("Verify retire response on failure")
    void verify_retireResponseOnFailure() {
        when(store.save(any())).thenReturn(StoreResult.alreadyExists("test"));
        var contractNegotiation = mock(ContractNegotiation.class);
        when(contractNegotiationService.search(any())).thenReturn(ServiceResult.success(List.of(contractNegotiation)));

        var result = service.retireAgreement(createAgreementsRetirementEntry());
        assertThat(result).isFailed();
        assertThat(result.reason()).isEqualTo(CONFLICT);
    }

    private static AgreementsRetirementEntry createAgreementsRetirementEntry() {
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