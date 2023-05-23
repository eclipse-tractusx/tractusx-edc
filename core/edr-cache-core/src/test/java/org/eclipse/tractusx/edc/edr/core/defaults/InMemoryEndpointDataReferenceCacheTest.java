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

package org.eclipse.tractusx.edc.edr.core.defaults;

import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.tractusx.edc.edr.spi.EndpointDataReferenceEntry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryEndpointDataReferenceCacheTest {
    private static final String TRANSFER_PROCESS_ID = "tp1";
    private static final String ASSET_ID = "asset1";
    private static final String AGREEMENT_ID = "agreement1";

    private static final String EDR_ID = "edr1";

    private final InMemoryEndpointDataReferenceCache cache = new InMemoryEndpointDataReferenceCache();

    @Test
    @SuppressWarnings("DataFlowIssue")
    void verify_operations() {
        var edr = EndpointDataReference.Builder.newInstance()
                .endpoint("http://test.com")
                .id(EDR_ID)
                .authCode("11111")
                .authKey("authentication").build();

        var entry = EndpointDataReferenceEntry.Builder.newInstance()
                .assetId(ASSET_ID)
                .agreementId(AGREEMENT_ID)
                .transferProcessId(TRANSFER_PROCESS_ID)
                .build();

        cache.save(entry, edr);

        assertThat(cache.resolveReference(TRANSFER_PROCESS_ID).getId()).isEqualTo(EDR_ID);

        var edrs = cache.referencesForAsset(ASSET_ID);
        assertThat(edrs.size()).isEqualTo(1);
        assertThat(edrs.get((0)).getId()).isEqualTo(EDR_ID);

        var entries = cache.entriesForAsset(ASSET_ID);
        assertThat(entries.size()).isEqualTo(1);
        assertThat(entries.get((0)).getAssetId()).isEqualTo(ASSET_ID);

        entries = cache.entriesForAgreement(AGREEMENT_ID);
        assertThat(entries.size()).isEqualTo(1);
        assertThat(entries.get((0)).getAgreementId()).isEqualTo(AGREEMENT_ID);

        assertThat(cache.deleteByTransferProcessId(TRANSFER_PROCESS_ID).succeeded()).isTrue();

        assertThat(cache.entriesForAsset(ASSET_ID)).isEmpty();
        assertThat(cache.resolveReference(TRANSFER_PROCESS_ID)).isNull();
    }
}
