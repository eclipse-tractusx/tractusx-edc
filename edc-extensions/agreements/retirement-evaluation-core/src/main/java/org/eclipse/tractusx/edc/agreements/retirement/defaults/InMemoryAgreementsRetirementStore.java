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

import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.query.QueryResolver;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.store.ReflectionBasedQueryResolver;
import org.eclipse.tractusx.edc.agreements.retirement.spi.store.AgreementsRetirementStore;
import org.eclipse.tractusx.edc.agreements.retirement.spi.types.AgreementsRetirementEntry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * In Memory implementation of a {@link AgreementsRetirementStore}.
 */
public class InMemoryAgreementsRetirementStore implements AgreementsRetirementStore {

    private final QueryResolver<AgreementsRetirementEntry> queryResolver;
    private final Map<String, AgreementsRetirementEntry> cache = new ConcurrentHashMap<>();

    public InMemoryAgreementsRetirementStore(CriterionOperatorRegistry criterionOperatorRegistry) {
        queryResolver = new ReflectionBasedQueryResolver<>(AgreementsRetirementEntry.class, criterionOperatorRegistry);
    }

    @Override
    public StoreResult<Void> save(AgreementsRetirementEntry entry) {
        if (cache.containsKey(entry.getAgreementId())) {
            return StoreResult.alreadyExists(ALREADY_EXISTS_TEMPLATE.formatted(entry.getAgreementId()));
        }
        cache.put(entry.getAgreementId(), entry);
        return StoreResult.success();
    }

    @Override
    public StoreResult<Void> delete(String contractAgreementId) {
        return cache.remove(contractAgreementId) == null ?
                StoreResult.notFound(NOT_FOUND_TEMPLATE.formatted(contractAgreementId)) :
                StoreResult.success();
    }

    @Override
    public Stream<AgreementsRetirementEntry> findRetiredAgreements(QuerySpec querySpec) {
        return queryResolver.query(cache.values().stream(), querySpec);
    }
}
