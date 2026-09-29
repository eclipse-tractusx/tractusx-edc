/********************************************************************************
 * Copyright (c) 2025 Cofinity-X GmbH
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

package org.eclipse.tractusx.edc.agreements.bpns.spi.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.eclipse.edc.spi.entity.Entity;

import static java.util.Objects.requireNonNull;

/**
 * Representation of a Contract Agreement Retirement entry, to be stored in the {@link org.eclipse.tractusx.edc.agreements.bpns.spi.store.AgreementsBpnsStore}.
 */
public class AgreementsBpnsEntry extends Entity {

    private String agreementId;
    private String providerBpn;
    private String consumerBpn;

    public AgreementsBpnsEntry() {}

    public String getAgreementId() {
        return agreementId;
    }

    public String getProviderBpn() {
        return providerBpn;
    }

    public String getConsumerBpn() {
        return consumerBpn;
    }

    public static class Builder extends Entity.Builder<AgreementsBpnsEntry, Builder> {

        private Builder() {
            super(new AgreementsBpnsEntry());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder withAgreementId(String agreementId) {
            this.entity.agreementId = agreementId;
            return this;
        }

        public Builder withProviderBpn(String providerBpn) {
            this.entity.providerBpn = providerBpn;
            return this;
        }

        public Builder withConsumerBpn(String consumerBpn) {
            this.entity.consumerBpn = consumerBpn;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public AgreementsBpnsEntry build() {
            super.build();
            requireNonNull(entity.agreementId);
            requireNonNull(entity.providerBpn);
            requireNonNull(entity.consumerBpn);

            return entity;
        }
    }
}
