package org.eclipse.tractusx.edc.agreements.retirement.defaults;

import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.tractusx.edc.agreements.retirement.spi.AgreementsRetirementStore;

import java.util.HashMap;
import java.util.Map;

public class InMemoryAgreementsRetirementStore implements AgreementsRetirementStore {

    private final Map<String, String> cache = new HashMap<>();

    @Override
    public StoreResult<Void> save(String contractAgreementId, String timestamp) {
        if (cache.containsKey(contractAgreementId)) {
            return StoreResult.alreadyExists(ALREADY_EXISTS_TEMPLATE.formatted(contractAgreementId));
        }
        cache.put(contractAgreementId, timestamp);
        return StoreResult.success();
    }

    @Override
    public StoreResult<Void> delete(String contractAgreementId) {
        return cache.remove(contractAgreementId) == null ?
                StoreResult.notFound(NOT_FOUND_TEMPLATE.formatted(contractAgreementId)) :
                StoreResult.success();
    }

    @Override
    public StoreResult<String> findRetiredAgreement(String contractAgreementId) {
        var entry = cache.get(contractAgreementId);
        return entry == null ?
                StoreResult.notFound(NOT_FOUND_TEMPLATE.formatted(contractAgreementId)) :
                StoreResult.success(entry);
    }
}
