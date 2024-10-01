package org.eclipse.tractusx.edc.agreements.retirement.defaults;

import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.query.QueryResolver;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.tractusx.edc.agreements.retirement.spi.store.AgreementsRetirementStore;
import org.eclipse.tractusx.edc.agreements.retirement.spi.types.AgreementsRetirementEntry;

import org.eclipse.edc.store.ReflectionBasedQueryResolver;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
    public StoreResult<List<AgreementsRetirementEntry>> findRetiredAgreements(QuerySpec querySpec) {
        return StoreResult.success(queryResolver.query(cache.values().stream(), querySpec).collect(Collectors.toList()));
    }
}
