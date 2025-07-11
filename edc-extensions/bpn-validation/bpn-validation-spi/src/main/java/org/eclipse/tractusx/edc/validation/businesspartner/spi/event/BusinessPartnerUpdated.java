/********************************************************************************
 * Copyright (c) 2025 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.validation.businesspartner.spi.event;

public class BusinessPartnerUpdated extends BusinessPartnerEvent {
    @Override
    public String name() {
        return "bpn.updated";
    }

    public static class Builder extends BusinessPartnerEvent.Builder<BusinessPartnerUpdated, BusinessPartnerUpdated.Builder> {

        private Builder() {
            super(new BusinessPartnerUpdated());
        }

        public static BusinessPartnerUpdated.Builder newInstance() {
            return new Builder();
        }

        @Override
        public BusinessPartnerUpdated.Builder self() {
            return this;
        }
    }
}
