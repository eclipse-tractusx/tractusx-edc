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

package org.eclipse.tractusx.edc.iam.dcp.cache.store;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiablePresentationContainer;
import org.eclipse.tractusx.edc.spi.dcp.VerifiablePresentationCacheEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.NOT_FOUND;
import static org.mockito.Mockito.mock;

class InMemoryVerifiablePresentationCacheStoreTest {

    private final String participantContextId = "participantContextId";
    private final String counterPartyDid = "did:web:other";
    private final List<String> scopes = List.of("scope1", "scope2");
    private final VerifiablePresentationContainer vp = mock();
    private final Instant cachedAt = mock();

    private InMemoryVerifiablePresentationCacheStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryVerifiablePresentationCacheStore();
    }

    @Nested
    public class Store {
        @Test
        void shouldStoreEntry() {
            var entry = cacheEntry();

            var storeResult = store.store(entry);
            assertThat(storeResult).isSucceeded();

            var queryResult = store.query(participantContextId, counterPartyDid, scopes);
            assertThat(queryResult).isSucceeded();
            assertThat(queryResult.getContent()).isEqualTo(entry);
        }

        @Test
        void shouldOverrideEntry_whenIdAlreadyExists() {
            var entry1 = cacheEntry();
            store.store(entry1);

            var queryResult = store.query(participantContextId, counterPartyDid, scopes);
            assertThat(queryResult).isSucceeded();
            assertThat(queryResult.getContent()).isEqualTo(entry1);

            var entry2 = cacheEntry(List.of());
            store.store(entry2);

            queryResult = store.query(participantContextId, counterPartyDid, scopes);
            assertThat(queryResult).isSucceeded();
            assertThat(queryResult.getContent())
                    .isEqualTo(entry2)
                    .isNotEqualTo(entry1);
        }
    }

    @Nested
    public class Query {
        @Test
        void shouldReturnEntry() {
            var entry = cacheEntry();
            store.store(entry);

            var queryResult = store.query(participantContextId, counterPartyDid, scopes);

            assertThat(queryResult).isSucceeded();
            assertThat(queryResult.getContent()).isEqualTo(entry);
        }

        @Test
        void shouldReturnNotFound_whenEntryNotFound() {
            var queryResult = store.query(participantContextId, counterPartyDid, scopes);

            assertThat(queryResult).isFailed();
            assertThat(queryResult.getFailure().getReason()).isEqualTo(NOT_FOUND);
        }
    }

    @Nested
    public class Remove {
        @Test
        void shouldRemoveSingleEntry() {
            store.store(cacheEntry());

            store.remove(participantContextId, counterPartyDid, scopes);

            var queryResult = store.query(participantContextId, counterPartyDid, scopes);
            assertThat(queryResult).isFailed();
            assertThat(queryResult.getFailure().getReason()).isEqualTo(NOT_FOUND);
        }

        @Test
        void shouldRemoveAllEntriesForParticipant() {
            var otherScope = "another-scope";
            var entry1 = cacheEntry();
            var entry2 = cacheEntry(otherScope);
            store.store(entry1);
            store.store(entry2);

            store.remove(participantContextId, counterPartyDid);

            var queryResult1 = store.query(participantContextId, counterPartyDid, scopes);
            assertThat(queryResult1).isFailed();
            assertThat(queryResult1.getFailure().getReason()).isEqualTo(NOT_FOUND);

            var queryResult2 = store.query(participantContextId, counterPartyDid, List.of(otherScope));
            assertThat(queryResult2).isFailed();
            assertThat(queryResult2.getFailure().getReason()).isEqualTo(NOT_FOUND);
        }

        @Test
        void shouldRemoveEntriesOnlyForParticipant() {
            var otherScope = "another-scope";
            store.store(cacheEntry());
            store.store(cacheEntry(otherScope));

            var otherParticipant = "did:web:different";
            var otherParticipantEntry = new VerifiablePresentationCacheEntry(participantContextId, otherParticipant, scopes, List.of(vp), cachedAt);
            store.store(otherParticipantEntry);

            store.remove(participantContextId, counterPartyDid);

            var queryResult = store.query(participantContextId, otherParticipant, scopes);
            assertThat(queryResult).isSucceeded();
            assertThat(queryResult.getContent()).isEqualTo(otherParticipantEntry);
        }

        @Test
        void shouldRemoveEntriesOnlyForSameParticpantContext() {
            var otherScope = "another-scope";
            store.store(cacheEntry());
            store.store(cacheEntry(otherScope));

            var otherParticipantContext = "other-participant-context";
            var otherParticipantContextEntry = new VerifiablePresentationCacheEntry(otherParticipantContext, counterPartyDid, scopes, List.of(vp), cachedAt);
            store.store(otherParticipantContextEntry);

            store.remove(participantContextId, counterPartyDid);

            var queryResult = store.query(otherParticipantContext, counterPartyDid, scopes);
            assertThat(queryResult).isSucceeded();
            assertThat(queryResult.getContent()).isEqualTo(otherParticipantContextEntry);
        }
    }

    private VerifiablePresentationCacheEntry cacheEntry() {
        return new VerifiablePresentationCacheEntry(participantContextId, counterPartyDid, scopes, List.of(vp), cachedAt);
    }

    private VerifiablePresentationCacheEntry cacheEntry(String scope) {
        return new VerifiablePresentationCacheEntry(participantContextId, counterPartyDid, List.of(scope), List.of(vp), cachedAt);
    }

    private VerifiablePresentationCacheEntry cacheEntry(List<VerifiablePresentationContainer> presentations) {
        return new VerifiablePresentationCacheEntry(participantContextId, counterPartyDid, scopes, presentations, cachedAt);
    }
}
