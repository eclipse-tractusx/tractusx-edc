/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.edr.core.defaults;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry;

/**
 * A wrapper to persist {@link EndpointDataReferenceEntry}s and {@link EndpointDataReference}s.
 */
public class PersistentCacheEntry {
    private EndpointDataReferenceEntry edrEntry;
    private EndpointDataReference edr;

    public PersistentCacheEntry(@JsonProperty("edrEntry") EndpointDataReferenceEntry edrEntry, @JsonProperty("edr") EndpointDataReference edr) {
        this.edrEntry = edrEntry;
        this.edr = edr;
    }

    public EndpointDataReferenceEntry getEdrEntry() {
        return edrEntry;
    }

    public EndpointDataReference getEdr() {
        return edr;
    }
}
