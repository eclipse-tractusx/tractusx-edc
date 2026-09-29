/*
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft
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
 */

package org.eclipse.tractusx.edc.edr.spi.index.lock;

import org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.time.Instant;

import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.EDR_PROPERTY_EXPIRES_IN;

public interface EndpointDataReferenceLock {
    StoreResult<Boolean> acquireLock(String edrId, DataAddress edr);

    StoreResult<Void> releaseLock(String edrId);

    default boolean isExpired(DataAddress edr, EndpointDataReferenceEntry edrEntry)  {
        var expiresInString = edr.getStringProperty(EDR_PROPERTY_EXPIRES_IN);
        if (expiresInString == null) {
            return false;
        }
        var expiresIn = Long.parseLong(expiresInString);
        var expiresAt = edrEntry.getCreatedAt() / 1000L + expiresIn;
        var expiresAtInstant = Instant.ofEpochSecond(expiresAt);

        return expiresAtInstant.isBefore(Instant.now());
    }
}
