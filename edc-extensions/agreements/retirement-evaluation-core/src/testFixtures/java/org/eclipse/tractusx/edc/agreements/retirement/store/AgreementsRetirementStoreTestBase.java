package org.eclipse.tractusx.edc.agreements.retirement.store;

import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.tractusx.edc.agreements.retirement.spi.store.AgreementsRetirementStore;
import org.eclipse.tractusx.edc.agreements.retirement.spi.types.AgreementsRetirementEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AgreementsRetirementStoreTestBase {

    private AgreementsRetirementStore store;

    @BeforeEach
    void setup(){
        store = getStore();
    }

    @Test
    void findRetiredAgreement() {
        var agreementId = "test-agreement-id";
        var entry = createRetiredAgreementEntry(agreementId, "mock-reason");
        store.save(entry);

        var query = createFilterQueryByAgreementId(agreementId);
        var result = store.findRetiredAgreements(query);
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
        var result = store.findRetiredAgreements(query);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void save_whenExists() {
        var entry = createRetiredAgreementEntry("test-agreement-id", "mock-reason");
        store.save(entry);
        assertThat(store.save(entry).succeeded()).isFalse();
    }

    @Test
    void delete() {
        var agreementId = "test-agreement-id";
        var entry = createRetiredAgreementEntry(agreementId, "mock-reason");
        store.save(entry);
        var delete = store.delete(agreementId);
        assertThat(delete.succeeded()).withFailMessage(delete::getFailureDetail).isTrue();
    }

    @Test
    void delete_notExist() {
        var agreementId = "test-agreement-id";
        assertThat(store.delete(agreementId).succeeded()).isFalse();
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
