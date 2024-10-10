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

package org.eclipse.tractusx.edc.agreements.retirement.store;

import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.tractusx.edc.agreements.retirement.spi.store.AgreementsRetirementStore;
import org.eclipse.tractusx.edc.agreements.retirement.spi.types.AgreementsRetirementEntry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AgreementsRetirementStoreTestBase {

    @Test
    void findRetiredAgreement() {
        var agreementId = "test-agreement-id";
        var entry = createRetiredAgreementEntry(agreementId, "mock-reason");
        getStore().save(entry);

        var query = createFilterQueryByAgreementId(agreementId);
        var result = getStore().findRetiredAgreements(query);
        assertThat(result.getContent())
                .isNotNull()
                .hasSize(1)
                .first()
                .extracting(AgreementsRetirementEntry::getAgreementId)
                .isEqualTo(agreementId);
    }

    @Test
    void findRetiredAgreement_notExists() {
        var agreementId = "test-agreement-not-exists";
        var query = createFilterQueryByAgreementId(agreementId);
        var result = getStore().findRetiredAgreements(query);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void save_whenExists() {
        var entry = createRetiredAgreementEntry("test-agreement-id", "mock-reason");
        getStore().save(entry);
        assertThat(getStore().save(entry).succeeded()).isFalse();
    }

    @Test
    void delete() {
        var agreementId = "test-agreement-id";
        var entry = createRetiredAgreementEntry(agreementId, "mock-reason");
        getStore().save(entry);
        var delete = getStore().delete(agreementId);
        assertThat(delete.succeeded()).withFailMessage(delete::getFailureDetail).isTrue();
    }

    @Test
    void delete_notExist() {
        var agreementId = "test-agreement-id";
        assertThat(getStore().delete(agreementId).succeeded()).isFalse();
    }

    private AgreementsRetirementEntry createRetiredAgreementEntry(String agreementId, String reason) {
        return AgreementsRetirementEntry.Builder.newInstance()
                .withAgreementId(agreementId)
                .withReason(reason)
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

    protected abstract AgreementsRetirementStore getStore();

}
