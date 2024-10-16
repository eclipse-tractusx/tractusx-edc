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

package org.eclipse.tractusx.edc.agreements.retirement.spi.service;

import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.tractusx.edc.agreements.retirement.spi.types.AgreementsRetirementEntry;

import java.util.List;

/**
 * Service interface that offers the necessary functionality for the Contract Agreement Retirement feature.
 */
public interface AgreementsRetirementService {

    /**
     * Within a given {@link PolicyContext}, verifies if the attached contract agreement exists in {@link org.eclipse.tractusx.edc.agreements.retirement.spi.store.AgreementsRetirementStore}.
     *
     * @param agreementId the contract agreement id to verify
     * @return true if it exists, false otherwise.
     */
    boolean isRetired(String agreementId);

    /**
     * Returns a list of {@link AgreementsRetirementEntry} entries matching a valid {@link QuerySpec}
     *
     * @param querySpec a valid {@link QuerySpec}
     * @return a list of {@link AgreementsRetirementEntry}
     */
    ServiceResult<List<AgreementsRetirementEntry>> findAll(QuerySpec querySpec);

    /**
     * Saves an {@link AgreementsRetirementEntry} in the {@link org.eclipse.tractusx.edc.agreements.retirement.spi.store.AgreementsRetirementStore}.
     *
     * @param entry a valid {@link AgreementsRetirementEntry}
     * @return ServiceResult successs, or a conflict failure if it already exists.
     */
    ServiceResult<Void> retireAgreement(AgreementsRetirementEntry entry);

    /**
     * Given a contract agreement id, removes its matching {@link AgreementsRetirementEntry} from the {@link org.eclipse.tractusx.edc.agreements.retirement.spi.store.AgreementsRetirementStore}.
     *
     * @param contractAgreementId the contract agreement id of the AgreementRetirementEntry to delete
     * @return StoreResult success, not found failure if entry not found.
     */
    ServiceResult<Void> reactivate(String contractAgreementId);
}
