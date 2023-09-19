/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.edr.spi;

import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreFailure;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.tractusx.edc.edr.spi.store.EndpointDataReferenceCache;
import org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.UUID.randomUUID;
import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.eclipse.tractusx.edc.edr.spi.TestFunctions.edr;
import static org.eclipse.tractusx.edc.edr.spi.TestFunctions.edrEntry;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntryStates.NEGOTIATED;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntryStates.REFRESHING;
import static org.hamcrest.Matchers.hasSize;

public abstract class EndpointDataReferenceCacheTestBase {

    public static final String CONNECTOR_NAME = "test-connector";

    @Test
    void save() {

        var tpId = "tp1";
        var assetId = "asset1";
        var edrId = "edr1";

        var edr = edr(edrId);
        var entry = edrEntry(assetId, randomUUID().toString(), tpId);

        getStore().save(entry, edr);

        assertThat(getStore().resolveReference(tpId))
                .isNotNull()
                .extracting(EndpointDataReference::getId)
                .isEqualTo(edrId);

        var edrs = getStore().referencesForAsset(assetId, null);
        assertThat(edrs.size()).isEqualTo(1);
        assertThat(edrs.get((0)).getId()).isEqualTo(edrId);


    }

    @Test
    void findByTransferProcessId() {
        var entry = edrEntry("assetId", "agreementId", "tpId");
        getStore().save(entry, edr("edrId"));

        assertThat(getStore().findByIdAndLease(entry.getTransferProcessId())).isNotNull();
    }

    @Test
    void queryEntries_noQuerySpec() {
        var all = IntStream.range(0, 10)
                .mapToObj(i -> edrEntry("assetId" + i, "agreementId" + i, "tpId" + i))
                .peek(entry -> getStore().save(entry, edr(entry.getTransferProcessId())))
                .collect(Collectors.toList());

        assertThat(getStore().queryForEntries(QuerySpec.none())).containsExactlyInAnyOrderElementsOf(all);
    }

    @Test
    void queryEntries_assetIdQuerySpec() {
        IntStream.range(0, 10)
                .mapToObj(i -> edrEntry("assetId" + i, "agreementId" + i, "tpId" + i))
                .forEach(entry -> getStore().save(entry, edr(entry.getTransferProcessId())));

        var entry = edrEntry("assetId", "agreementId", "tpId");
        getStore().save(entry, edr("edrId"));

        var filter = Criterion.Builder.newInstance()
                .operandLeft("assetId")
                .operator("=")
                .operandRight(entry.getAssetId())
                .build();

        assertThat(getStore().queryForEntries(QuerySpec.Builder.newInstance().filter(filter).build())).containsOnly(entry);
    }

    @Test
    void queryEntries_agreementIdQuerySpec() {
        IntStream.range(0, 10)
                .mapToObj(i -> edrEntry("assetId" + i, "agreementId" + i, "tpId" + i))
                .forEach(entry -> getStore().save(entry, edr(entry.getTransferProcessId())));

        var entry = edrEntry("assetId", "agreementId", "tpId");
        getStore().save(entry, edr("edrId"));

        var filter = Criterion.Builder.newInstance()
                .operandLeft("agreementId")
                .operator("=")
                .operandRight(entry.getAgreementId())
                .build();

        assertThat(getStore().queryForEntries(QuerySpec.Builder.newInstance().filter(filter).build())).containsOnly(entry);
    }

    @Test
    void queryEntries_providerIdQuerySpec() {
        IntStream.range(0, 10)
                .mapToObj(i -> edrEntry("assetId" + i, "agreementId" + i, "tpId" + i))
                .forEach(entry -> getStore().save(entry, edr(entry.getTransferProcessId())));

        var entry = edrEntry("assetId", "agreementId", "tpId");
        getStore().save(entry, edr("edrId"));

        var filter = Criterion.Builder.newInstance()
                .operandLeft("providerId")
                .operator("=")
                .operandRight(entry.getProviderId())
                .build();

        assertThat(getStore().queryForEntries(QuerySpec.Builder.newInstance().filter(filter).build())).containsOnly(entry);
    }

    @Test
    void deleteByTransferProcessId_shouldDelete_WhenFound() {

        var entry = edrEntry("assetId", "agreementId", "tpId");
        getStore().save(entry, edr("edrId"));

        assertThat(getStore().deleteByTransferProcessId(entry.getTransferProcessId()))
                .extracting(StoreResult::getContent)
                .isEqualTo(entry);

        assertThat(getStore().resolveReference(entry.getTransferProcessId())).isNull();
        assertThat(getStore().referencesForAsset(entry.getAssetId(), entry.getProviderId())).hasSize(0);
        assertThat(getStore().queryForEntries(QuerySpec.max())).hasSize(0);

    }

    @Test
    void deleteByTransferProcessId_shouldReturnError_whenNotFound() {
        assertThat(getStore().deleteByTransferProcessId("notFound"))
                .extracting(StoreResult::reason)
                .isEqualTo(StoreFailure.Reason.NOT_FOUND);
    }

    @Test
    void nextNotLeased() {
        var all = IntStream.range(0, 10)
                .mapToObj(i -> edrEntry("assetId" + i, "agreementId" + i, "tpId" + i))
                .peek((entry -> getStore().save(entry, edr(entry.getTransferProcessId()))))
                .toList();

        assertThat(getStore().nextNotLeased(5, hasState(NEGOTIATED.code())))
                .hasSize(5)
                .extracting(EndpointDataReferenceEntry::getTransferProcessId)
                .isSubsetOf(all.stream().map(EndpointDataReferenceEntry::getTransferProcessId).collect(Collectors.toList()))
                .allMatch(id -> isLockedBy(id, CONNECTOR_NAME));
    }

    @Test
    void nextNotLeased_shouldOnlyReturnFreeItems() {
        var all = IntStream.range(0, 10)
                .mapToObj(i -> edrEntry("assetId" + i, "agreementId" + i, "tpId" + i))
                .peek((entry -> getStore().save(entry, edr(entry.getTransferProcessId()))))
                .collect(Collectors.toList());

        // lease a few
        var leasedTp = all.stream().skip(5).peek(tp -> lockEntity(tp.getId(), CONNECTOR_NAME)).toList();

        // should not contain leased TPs
        assertThat(getStore().nextNotLeased(10, hasState(NEGOTIATED.code())))
                .hasSize(5)
                .isSubsetOf(all)
                .doesNotContainAnyElementsOf(leasedTp);
    }

    @Test
    void nextNotLeased_noFreeItem_shouldReturnEmpty() {
        var state = NEGOTIATED;
        range(0, 3)
                .mapToObj(i -> edrEntry("assetId" + i, "agreementId" + i, "tpId" + i))
                .forEach((entry -> getStore().save(entry, edr(entry.getTransferProcessId()))));

        // first time works
        assertThat(getStore().nextNotLeased(10, hasState(state.code()))).hasSize(3);
        // second time returns empty list
        assertThat(getStore().nextNotLeased(10, hasState(state.code()))).isEmpty();
    }

    @Test
    void nextNotLeased_noneInDesiredState() {
        range(0, 3)
                .mapToObj(i -> edrEntry("assetId" + i, "agreementId" + i, "tpId" + i))
                .forEach((entry -> getStore().save(entry, edr(entry.getTransferProcessId()))));


        var nextNotLeased = getStore().nextNotLeased(10, hasState(REFRESHING.code()));

        assertThat(nextNotLeased).isEmpty();
    }

    @Test
    void nextNotLeased_batchSizeLimits() {
        range(0, 10)
                .mapToObj(i -> edrEntry("assetId" + i, "agreementId" + i, "tpId" + i))
                .forEach((entry -> getStore().save(entry, edr(entry.getTransferProcessId()))));


        // first time works
        var result = getStore().nextNotLeased(3, hasState(NEGOTIATED.code()));
        assertThat(result).hasSize(3);
    }

    @Test
    void nextNotLeased_verifyTemporalOrdering() {
        range(0, 10)
                .mapToObj(i -> edrEntry("assetId" + i, "agreementId" + i, String.valueOf(i)))
                .peek(this::delayByTenMillis)
                .forEach((entry -> getStore().save(entry, edr(entry.getTransferProcessId()))));

        assertThat(getStore().nextNotLeased(20, hasState(NEGOTIATED.code())))
                .extracting(EndpointDataReferenceEntry::getId)
                .map(Integer::parseInt)
                .isSortedAccordingTo(Integer::compareTo);
    }

    @Test
    void nextNotLeased_verifyMostRecentlyUpdatedIsLast() throws InterruptedException {
        var all = range(0, 10)
                .mapToObj(i -> edrEntry("assetId" + i, "agreementId" + i, "tpId" + i))
                .peek((entry -> getStore().save(entry, edr(entry.getTransferProcessId()))))
                .toList();

        Thread.sleep(100);

        var fourth = all.get(3);
        fourth.updateStateTimestamp();
        getStore().update(fourth);

        var next = getStore().nextNotLeased(20, hasState(NEGOTIATED.code()));
        assertThat(next.indexOf(fourth)).isEqualTo(9);
    }

    @Test
    @DisplayName("Verifies that calling nextNotLeased locks the TP for any subsequent calls")
    void nextNotLeased_locksEntity() {
        var entry = edrEntry("assetId", "agreementId", "tpId");
        getStore().save(entry, edr("edrId"));

        getStore().nextNotLeased(100, hasState(NEGOTIATED.code()));

        assertThat(isLockedBy(entry.getId(), CONNECTOR_NAME)).isTrue();
    }

    @Test
    void nextNotLeased_expiredLease() {
        var entry = edrEntry("assetId", "agreementId", "tpId");
        getStore().save(entry, edr("edrId"));

        lockEntity(entry.getId(), CONNECTOR_NAME, Duration.ofMillis(100));

        await().atLeast(Duration.ofMillis(100))
                .atMost(Duration.ofMillis(500))
                .until(() -> getStore().nextNotLeased(10, hasState(NEGOTIATED.code())), hasSize(1));
    }

    @Test
    void nextNotLeased_shouldLeaseEntityUntilUpdate() {
        var entry = edrEntry("assetId", "agreementId", "tpId");
        getStore().save(entry, edr("edrId"));

        var firstQueryResult = getStore().nextNotLeased(1, hasState(NEGOTIATED.code()));
        assertThat(firstQueryResult).hasSize(1);

        var secondQueryResult = getStore().nextNotLeased(1, hasState(NEGOTIATED.code()));
        assertThat(secondQueryResult).hasSize(0);

        var retrieved = firstQueryResult.get(0);
        getStore().update(retrieved);

        var thirdQueryResult = getStore().nextNotLeased(1, hasState(NEGOTIATED.code()));
        assertThat(thirdQueryResult).hasSize(1);
    }

    @Test
    void nextNotLeased_avoidsStarvation() throws InterruptedException {

        range(0, 10)
                .mapToObj(i -> edrEntry("assetId" + i, "agreementId" + i, "tpId" + i))
                .forEach((entry -> getStore().save(entry, edr(entry.getTransferProcessId()))));

        var list1 = getStore().nextNotLeased(5, hasState(NEGOTIATED.code()));
        Thread.sleep(50); //simulate a short delay to generate different timestamps
        list1.forEach(tp -> {
            tp.updateStateTimestamp();
            getStore().update(tp);
        });
        var list2 = getStore().nextNotLeased(5, hasState(NEGOTIATED.code()));
        assertThat(list1).isNotEqualTo(list2).doesNotContainAnyElementsOf(list2);
    }

    @Test
    @DisplayName("Verify that the lease on a TP is cleared by an update")
    void update_shouldBreakLease() {
        var entry = edrEntry("assetId", "agreementId", "tpId");
        getStore().save(entry, edr("edrId"));
        // acquire lease
        lockEntity(entry.getId(), CONNECTOR_NAME);

        entry.transitionToRefreshing(); //modify
        getStore().update(entry);

        // lease should be broken
        var notLeased = getStore().nextNotLeased(10, hasState(REFRESHING.code()));

        assertThat(notLeased).usingRecursiveFieldByFieldElementComparator().containsExactly(entry);
    }

    @Test
    void update_leasedByOther_shouldThrowException() {
        var entry = edrEntry("assetId", "agreementId", "tpId");
        getStore().save(entry, edr("edrId"));

        lockEntity(entry.getId(), "someone");

        entry.transitionToRefreshing();

        // leased by someone else -> throw exception
        assertThatThrownBy(() -> getStore().update(entry)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void delete_isLeasedBySelf_shouldThrowException() {
        var entry = edrEntry("assetId", "agreementId", "tpId");
        getStore().save(entry, edr("edrId"));

        lockEntity(entry.getId(), CONNECTOR_NAME);


        assertThatThrownBy(() -> getStore().deleteByTransferProcessId(entry.getTransferProcessId())).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void delete_isLeasedByOther_shouldThrowException() {
        var entry = edrEntry("assetId", "agreementId", "tpId");
        getStore().save(entry, edr("edrId"));

        lockEntity(entry.getId(), "someone-else");

        assertThatThrownBy(() -> getStore().deleteByTransferProcessId(entry.getTransferProcessId())).isInstanceOf(IllegalStateException.class);
    }

    protected abstract EndpointDataReferenceCache getStore();

    protected abstract void lockEntity(String negotiationId, String owner, Duration duration);

    protected void lockEntity(String negotiationId, String owner) {
        lockEntity(negotiationId, owner, Duration.ofSeconds(60));
    }

    protected abstract boolean isLockedBy(String negotiationId, String owner);

    private void delayByTenMillis(EndpointDataReferenceEntry t) {
        try {
            Thread.sleep(10);
        } catch (InterruptedException ignored) {
            // noop
        }
        t.updateStateTimestamp();
    }
}
