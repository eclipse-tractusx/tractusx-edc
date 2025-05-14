/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.validation.businesspartner.defaults;

import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.tractusx.edc.validation.businesspartner.spi.BusinessPartnerStore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InMemoryBusinessPartnerStore implements BusinessPartnerStore {
    private final Map<String, List<String>> cache = new HashMap<>();

    @Override
    public StoreResult<List<String>> resolveForBpn(String businessPartnerNumber) {
        var entry = cache.get(businessPartnerNumber);
        return entry == null ?
                StoreResult.notFound(NOT_FOUND_TEMPLATE.formatted(businessPartnerNumber)) :
                StoreResult.success(entry);
    }

    @Override
    public StoreResult<List<String>> resolveForBpnGroup(String businessPartnerGroup) {
        var bpns = cache.entrySet().stream()
                .filter(bpn -> bpn.getValue().stream().anyMatch(groups -> groups.contains(businessPartnerGroup)))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        return bpns.isEmpty() ?
                StoreResult.notFound(NOT_FOUND_TEMPLATE.formatted(businessPartnerGroup)) :
                StoreResult.success(bpns);
    }

    @Override
    public StoreResult<List<String>> resolveForBpnGroups() {
        var groups = cache.values().stream()
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.toList());
        return StoreResult.success(groups);
    }

    @Override
    public StoreResult<Void> save(String businessPartnerNumber, List<String> groups) {
        //to maintain behavioural consistency with the SQL store
        if (cache.containsKey(businessPartnerNumber)) {
            return StoreResult.alreadyExists(ALREADY_EXISTS_TEMPLATE.formatted(businessPartnerNumber));
        }
        cache.put(businessPartnerNumber, groups);
        return StoreResult.success();
    }

    @Override
    public StoreResult<Void> delete(String businessPartnerNumber) {

        return cache.remove(businessPartnerNumber) == null ?
                StoreResult.notFound(NOT_FOUND_TEMPLATE.formatted(businessPartnerNumber)) :
                StoreResult.success();
    }

    @Override
    public StoreResult<Void> update(String businessPartnerNumber, List<String> groups) {
        if (cache.containsKey(businessPartnerNumber)) {
            cache.put(businessPartnerNumber, groups);
            return StoreResult.success();
        }
        return StoreResult.notFound(NOT_FOUND_TEMPLATE.formatted(businessPartnerNumber));
    }
}
