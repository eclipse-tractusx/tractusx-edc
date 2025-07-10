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

import org.eclipse.edc.spi.event.Event;

import java.util.List;

public abstract class BusinessPartnerEvent extends Event {

    protected String businessPartnerNumber;
    protected List<String> businessPartnerGroups;

    public String getBusinessPartnerNumber() {
        return businessPartnerNumber;
    }

    public List<String> getBusinessPartnerGroups() {
        return businessPartnerGroups;
    }

    public abstract static class Builder<T extends BusinessPartnerEvent, B extends BusinessPartnerEvent.Builder<T, B>> {

        protected final T event;

        protected Builder(T event) {
            this.event = event;
        }

        public abstract B self();

        public B businessPartnerNumber(String businessPartnerNumber) {
            event.businessPartnerNumber = businessPartnerNumber;
            return self();
        }

        public B businessPartnerGroups(List<String> businessPartnerGroups) {
            event.businessPartnerGroups = businessPartnerGroups;
            return self();
        }

        public T build() {
            return event;
        }
    }
}
