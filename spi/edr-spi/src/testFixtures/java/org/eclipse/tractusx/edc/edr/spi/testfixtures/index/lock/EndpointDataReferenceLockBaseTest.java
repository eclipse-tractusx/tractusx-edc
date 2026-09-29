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

package org.eclipse.tractusx.edc.edr.spi.testfixtures.index.lock;

import org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.tractusx.edc.edr.spi.CoreConstants;
import org.eclipse.tractusx.edc.edr.spi.index.lock.EndpointDataReferenceLock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;


public abstract class EndpointDataReferenceLockBaseTest {

    protected static final String ACQUIRE_LOCK_TP = "acquire-lock-tp";

    protected abstract EndpointDataReferenceLock getStore();

    @Test
    @DisplayName("Verify acquireLock returns true when expired and leaves with lock")
    void verify_acquireLockReturnsTrueWhenExpired() {

        var result = getStore().acquireLock(ACQUIRE_LOCK_TP, edr("-1000"));

        assertThat(result).isSucceeded();
        assertThat(result.getContent()).isTrue();
        assertThat(getStore().releaseLock(ACQUIRE_LOCK_TP)).isSucceeded();
    }

    @Test
    @DisplayName("Verify acquireLock returns false when not expired and leaves with lock")
    void verify_acquireLockReturnsFalseWhenNotExpired() {

        var edr = edr("2000");

        var result = getStore().acquireLock(ACQUIRE_LOCK_TP, edr);

        assertThat(result).isSucceeded();
        assertThat(result.getContent()).isFalse();
        assertThat(getStore().releaseLock(ACQUIRE_LOCK_TP)).isSucceeded();

    }

    @Test
    @DisplayName("Verify release lock returns true when release")
    void verify_releaseLockReturnsSuccessWhenReleased() {

        getStore().acquireLock(ACQUIRE_LOCK_TP, edr("2000"));

        var result = getStore().releaseLock(ACQUIRE_LOCK_TP);
        assertThat(result).isSucceeded();

    }

    @Test
    @DisplayName("Verify isExpired Returns true when expired")
    void verify_isExpiredReturnTrueWhenExpired() {
        var result = getStore().isExpired(edr("-1000"), edrEntry("mock", "mock"));
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Verify isExpired Returns false when not expired")
    void verify_isExpiredReturnFalseWhenNotExpired() {
        var result = getStore().isExpired(edr("1000"), edrEntry("mock", "mock"));
        assertThat(result).isFalse();
    }

    protected DataAddress edr(String expireIn) {
        return DataAddress.Builder.newInstance().type("test").property(CoreConstants.TX_AUTH_NS + "expiresIn", expireIn).build();
    }

    protected EndpointDataReferenceEntry edrEntry(String assetId, String transferProcessId) {
        return EndpointDataReferenceEntry.Builder.newInstance()
                .assetId(assetId)
                .transferProcessId(transferProcessId)
                .contractNegotiationId(UUID.randomUUID().toString())
                .agreementId(UUID.randomUUID().toString())
                .providerId(UUID.randomUUID().toString())
                .build();
    }

}