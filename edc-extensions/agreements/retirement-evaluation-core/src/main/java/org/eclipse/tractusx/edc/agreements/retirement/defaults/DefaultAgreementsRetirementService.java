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

package org.eclipse.tractusx.edc.agreements.retirement.defaults;

import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.tractusx.edc.agreements.retirement.spi.service.AgreementsRetirementService;
import org.eclipse.tractusx.edc.agreements.retirement.spi.store.AgreementsRetirementStore;
import org.eclipse.tractusx.edc.agreements.retirement.spi.types.AgreementsRetirementEntry;

import java.util.List;

/**
 * Default implementation for a {@link AgreementsRetirementService}.
 */
public class DefaultAgreementsRetirementService implements AgreementsRetirementService {

    private final AgreementsRetirementStore store;
    private final TransactionContext transactionContext;

    public DefaultAgreementsRetirementService(AgreementsRetirementStore store, TransactionContext transactionContext) {
        this.store = store;
        this.transactionContext = transactionContext;
    }

    @Override
    public boolean isRetired(String agreementId) {
        return transactionContext.execute(() -> {
            var result = store.findRetiredAgreements(createFilterQueryByAgreementId(agreementId));
            if (result.succeeded()) {
                return !result.getContent().isEmpty();
            }
            return false;
        });
    }

    @Override
    public ServiceResult<List<AgreementsRetirementEntry>> findAll(QuerySpec querySpec) {
        return transactionContext.execute(() -> ServiceResult.from(store.findRetiredAgreements(querySpec)));
    }

    @Override
    public ServiceResult<Void> retireAgreement(AgreementsRetirementEntry entry) {
        return transactionContext.execute(() -> ServiceResult.from(store.save(entry)));
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
