package org.eclipse.tractusx.edc.agreements.retirement.store;

import org.eclipse.tractusx.edc.agreements.retirement.spi.AgreementsRetirementStore;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AgreementsRetirementStoreTestBase {

    @Test
    void findRetiredAgreement() {
        getStore().save("test-agreement-id", "test-timestamp");
        assertThat(getStore().findRetiredAgreement("test-agreement-id").getContent()).isEqualTo("test-timestamp");
    }

    @Test
    void findRetiredAgreement_notExists() {
        assertThat(getStore().findRetiredAgreement("test-agreement-id").succeeded()).isFalse();
    }

    @Test
    void save_whenExists() {
        getStore().save("test-agreement-id", "test-timestamp");
        assertThat(getStore().save("test-agreement-id", "test-timestamp").succeeded()).isFalse();
    }

    @Test
    void delete() {
        var agreementId = "test-agreement-id";
        getStore().save(agreementId, "test-timestamp");
        var delete = getStore().delete(agreementId);
        assertThat(delete.succeeded()).withFailMessage(delete::getFailureDetail).isTrue();
    }

    @Test
    void delete_notExist() {
        var businessPartnerNumber = "test-agreement-id";
        getStore().delete(businessPartnerNumber);
        assertThat(getStore().findRetiredAgreement(businessPartnerNumber).succeeded()).isFalse();
    }

    protected abstract AgreementsRetirementStore getStore();

}
