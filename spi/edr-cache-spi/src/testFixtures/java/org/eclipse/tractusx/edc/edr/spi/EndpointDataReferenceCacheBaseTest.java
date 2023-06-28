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
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.edr.spi.TestFunctions.edr;
import static org.eclipse.tractusx.edc.edr.spi.TestFunctions.edrEntry;

public abstract class EndpointDataReferenceCacheBaseTest {

    protected abstract EndpointDataReferenceCache getStore();

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

        var edrs = getStore().referencesForAsset(assetId);
        assertThat(edrs.size()).isEqualTo(1);
        assertThat(edrs.get((0)).getId()).isEqualTo(edrId);

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
    void deleteByTransferProcessId_shouldDelete_WhenFound() {

        var entry = edrEntry("assetId", "agreementId", "tpId");
        getStore().save(entry, edr("edrId"));

        assertThat(getStore().deleteByTransferProcessId(entry.getTransferProcessId()))
                .extracting(StoreResult::getContent)
                .isEqualTo(entry);

        assertThat(getStore().resolveReference(entry.getTransferProcessId())).isNull();
        assertThat(getStore().referencesForAsset(entry.getAssetId())).hasSize(0);
        assertThat(getStore().queryForEntries(QuerySpec.max())).hasSize(0);

    }

    @Test
    void deleteByTransferProcessId_shouldReturnError_whenNotFound() {
        assertThat(getStore().deleteByTransferProcessId("notFound"))
                .extracting(StoreResult::reason)
                .isEqualTo(StoreFailure.Reason.NOT_FOUND);
    }

}
