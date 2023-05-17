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

package org.eclipse.tractusx.edc.api.cp.adapter.transform;

import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.api.cp.adapter.dto.EndpointDataReferenceEntryDto.Builder;
import static org.eclipse.tractusx.edc.api.cp.adapter.dto.EndpointDataReferenceEntryDto.EDR_ENTRY_DTO_AGREEMENT_ID;
import static org.eclipse.tractusx.edc.api.cp.adapter.dto.EndpointDataReferenceEntryDto.EDR_ENTRY_DTO_ASSET_ID;
import static org.eclipse.tractusx.edc.api.cp.adapter.dto.EndpointDataReferenceEntryDto.EDR_ENTRY_TRANSFER_PROCESS_ID;
import static org.mockito.Mockito.mock;

class JsonObjectFromEndpointDataReferenceEntryDtoTransformerTest {

    private final TransformerContext context = mock(TransformerContext.class);
    private JsonObjectFromEndpointDataReferenceEntryDtoTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectFromEndpointDataReferenceEntryDtoTransformer();
    }

    @Test
    void transform() {

        var dto = Builder.newInstance()
                .assetId("id")
                .transferProcessId("tpId")
                .agreementId("aId")
                .build();

        var jsonObject = transformer.transform(dto, context);

        assertThat(jsonObject).isNotNull();
        assertThat(jsonObject.getJsonString(EDR_ENTRY_DTO_AGREEMENT_ID).getString()).isNotNull().isEqualTo(dto.getAgreementId());
        assertThat(jsonObject.getJsonString(EDR_ENTRY_DTO_ASSET_ID).getString()).isNotNull().isEqualTo(dto.getAssetId());
        assertThat(jsonObject.getJsonString(EDR_ENTRY_TRANSFER_PROCESS_ID).getString()).isNotNull().isEqualTo(dto.getTransferProcessId());
    }
}