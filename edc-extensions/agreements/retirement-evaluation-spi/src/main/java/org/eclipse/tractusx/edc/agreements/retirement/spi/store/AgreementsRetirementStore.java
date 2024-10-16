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

package org.eclipse.tractusx.edc.agreements.retirement.spi.store;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.tractusx.edc.agreements.retirement.spi.types.AgreementsRetirementEntry;

import java.util.List;
import java.util.stream.Stream;

/**
 * Interface for managing the storage point of {@link AgreementsRetirementEntry}.
 */
@ExtensionPoint
public interface AgreementsRetirementStore  {
    String NOT_FOUND_TEMPLATE = "Contract Agreement with %s was not found on retirement list.";
    String ALREADY_EXISTS_TEMPLATE = "Contract Agreement %s is already retired.";

    /**
     * Saves an AgreementsRetirementEntry in the store.
     *
     * @param entry {@link AgreementsRetirementEntry}
     * @return StoreResult success, already exists failure if already exists
     */
    StoreResult<Void> save(AgreementsRetirementEntry entry);

    /**
     * Deletes an AgreementsRetirementEntry from the store, given a contract agreement id.
     *
     * @param contractAgreementId the contract agreement id of the AgreementRetirementEntry to delete
     * @return StoreResult success, not found failure if entry not found.
     */
    StoreResult<Void> delete(String contractAgreementId);

    /**
     * Returns a list of AgreementRetirementEntry matching a query spec.
     *
     * @param querySpec a valid {@link QuerySpec}
     * @return a list of AgreementRetirementEntry entries.
     */
    Stream<AgreementsRetirementEntry> findRetiredAgreements(QuerySpec querySpec);

}
