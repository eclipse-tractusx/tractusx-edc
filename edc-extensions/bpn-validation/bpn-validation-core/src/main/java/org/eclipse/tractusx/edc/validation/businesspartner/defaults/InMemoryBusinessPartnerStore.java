/*
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.validation.businesspartner.defaults;

import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.tractusx.edc.validation.businesspartner.spi.BusinessPartnerStore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
