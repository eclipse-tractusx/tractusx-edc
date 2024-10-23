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

package org.eclipse.tractusx.edc.agreements.retirement.spi.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.eclipse.edc.spi.entity.Entity;

import static java.util.Objects.requireNonNull;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_NAMESPACE;

/**
 * Representation of a Contract Agreement Retirement entry, to be stored in the {@link org.eclipse.tractusx.edc.agreements.retirement.spi.store.AgreementsRetirementStore}.
 */
public class AgreementsRetirementEntry extends Entity {

    public static final String AR_ENTRY_TYPE = EDC_NAMESPACE + "AgreementsRetirementEntry";

    public static final String AR_ENTRY_AGREEMENT_ID = EDC_NAMESPACE + "agreementId";
    public static final String AR_ENTRY_REASON = TX_NAMESPACE + "reason";
    public static final String AR_ENTRY_RETIREMENT_DATE = TX_NAMESPACE + "agreementRetirementDate";

    private String agreementId;
    private String reason;
    private long agreementRetirementDate = 0L;

    public AgreementsRetirementEntry() {}

    public String getAgreementId() {
        return agreementId;
    }

    public String getReason() {
        return reason;
    }

    public long getAgreementRetirementDate() {
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

        public Builder withAgreementRetirementDate(long agreementRetirementDate) {
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

            if (entity.agreementRetirementDate == 0L) {
                entity.agreementRetirementDate = this.entity.clock.instant().getEpochSecond();
            }

            return entity;
        }
    }
}
