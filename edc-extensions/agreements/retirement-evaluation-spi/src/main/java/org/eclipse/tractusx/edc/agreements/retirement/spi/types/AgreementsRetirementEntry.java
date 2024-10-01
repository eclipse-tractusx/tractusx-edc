package org.eclipse.tractusx.edc.agreements.retirement.spi.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.eclipse.edc.spi.entity.Entity;

import java.time.Instant;

import static java.util.Objects.requireNonNull;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

public class AgreementsRetirementEntry extends Entity {

    public static final String AR_ENTRY_TYPE = EDC_NAMESPACE + "AgreementsRetirementEntry";

    public static final String AR_ENTRY_AGREEMENT_ID = EDC_NAMESPACE + "agreementId";
    public static final String AR_ENTRY_REASON = EDC_NAMESPACE + "reason";
    public static final String AR_ENTRY_RETIREMENT_DATE = EDC_NAMESPACE + "agreementRetirementDate";

    private String agreementId;
    private String reason;
    private String agreementRetirementDate;

    public AgreementsRetirementEntry() {}

    public String getAgreementId() {
        return agreementId;
    }

    public String getReason() {
        return reason;
    }

    public String getAgreementRetirementDate() {
        return agreementRetirementDate;
    }

    public static class Builder extends Entity.Builder<AgreementsRetirementEntry, AgreementsRetirementEntry.Builder> {

        private Builder() {
            super(new AgreementsRetirementEntry());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder withAgreementId(String agreementId) {
            this.entity.agreementId = agreementId;
            return this;
        }

        public Builder withReason(String reason) {
            this.entity.reason = reason;
            return this;
        }

        public Builder withAgreementRetirementDate(String agreementRetirementDate) {
            this.entity.agreementRetirementDate = agreementRetirementDate;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public AgreementsRetirementEntry build() {
            super.build();
            requireNonNull(entity.agreementId, AR_ENTRY_AGREEMENT_ID);
            requireNonNull(entity.reason, AR_ENTRY_REASON);

            if (entity.agreementRetirementDate == null) {
                entity.agreementRetirementDate = Instant.now().toString();
            }

            return entity;
        }
    }
}
