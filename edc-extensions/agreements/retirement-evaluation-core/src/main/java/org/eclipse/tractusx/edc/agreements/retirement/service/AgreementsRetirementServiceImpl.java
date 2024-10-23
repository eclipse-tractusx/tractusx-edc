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

import org.eclipse.edc.connector.controlplane.services.spi.contractagreement.ContractAgreementService;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.tractusx.edc.agreements.retirement.spi.service.AgreementsRetirementService;
import org.eclipse.tractusx.edc.agreements.retirement.spi.store.AgreementsRetirementStore;
import org.eclipse.tractusx.edc.agreements.retirement.spi.types.AgreementsRetirementEntry;

import java.util.List;
import java.util.stream.Collectors;

import static org.eclipse.tractusx.edc.agreements.retirement.spi.store.AgreementsRetirementStore.NOT_FOUND_IN_CONTRACT_AGREEMENT_TEMPLATE;

/**
 * Implementation for the {@link AgreementsRetirementService}.
 */
public class AgreementsRetirementServiceImpl implements AgreementsRetirementService {

    private final AgreementsRetirementStore store;
    private final TransactionContext transactionContext;
    private final ContractAgreementService contractAgreementService;

    public AgreementsRetirementServiceImpl(AgreementsRetirementStore store, TransactionContext transactionContext, ContractAgreementService contractAgreementService) {
        this.store = store;
        this.transactionContext = transactionContext;
        this.contractAgreementService = contractAgreementService;
    }

    @Override
    public boolean isRetired(String agreementId) {
        return transactionContext.execute(() -> store.findRetiredAgreements(createFilterQueryByAgreementId(agreementId))
                .findAny()
                .isPresent());
    }

    @Override
    public ServiceResult<List<AgreementsRetirementEntry>> findAll(QuerySpec querySpec) {
        return transactionContext.execute(() -> ServiceResult.success(store.findRetiredAgreements(querySpec).collect(Collectors.toList())));
    }

    @Override
    public ServiceResult<Void> retireAgreement(AgreementsRetirementEntry entry) {
        return transactionContext.execute(() ->
                contractAgreementService.findById(entry.getAgreementId()) != null
                        ? ServiceResult.from(store.save(entry))
                        : ServiceResult.notFound(NOT_FOUND_IN_CONTRACT_AGREEMENT_TEMPLATE.formatted(entry.getAgreementId())));
    }

    @Override
    public ServiceResult<Void> reactivate(String contractAgreementId) {
        return transactionContext.execute(() -> ServiceResult.from(store.delete(contractAgreementId)));
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
