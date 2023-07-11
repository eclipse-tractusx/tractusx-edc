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

package org.eclipse.tractusx.edc.api.edr.transform;

import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_AGREEMENT_ID;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_ASSET_ID;
import static org.eclipse.tractusx.edc.edr.spi.types.EndpointDataReferenceEntry.EDR_ENTRY_TRANSFER_PROCESS_ID;
import static org.mockito.Mockito.mock;

class JsonObjectFromEndpointDataReferenceEntryTransformerTest {

    private final TransformerContext context = mock(TransformerContext.class);
    private JsonObjectFromEndpointDataReferenceEntryTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectFromEndpointDataReferenceEntryTransformer();
    }

    @Test
    void transform() {

        var dto = EndpointDataReferenceEntry.Builder.newInstance()
                .assetId("id")
                .transferProcessId("tpId")
                .agreementId("aId")
                .build();

        var jsonObject = transformer.transform(dto, context);

        assertThat(jsonObject).isNotNull();
        assertThat(jsonObject.getJsonString(EDR_ENTRY_AGREEMENT_ID).getString()).isNotNull().isEqualTo(dto.getAgreementId());
        assertThat(jsonObject.getJsonString(EDR_ENTRY_ASSET_ID).getString()).isNotNull().isEqualTo(dto.getAssetId());
        assertThat(jsonObject.getJsonString(EDR_ENTRY_TRANSFER_PROCESS_ID).getString()).isNotNull().isEqualTo(dto.getTransferProcessId());
    }
}