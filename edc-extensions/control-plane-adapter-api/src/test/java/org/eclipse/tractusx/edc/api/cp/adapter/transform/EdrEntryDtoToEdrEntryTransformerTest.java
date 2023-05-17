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
import org.eclipse.tractusx.edc.edr.spi.EndpointDataReferenceEntry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class EdrEntryDtoToEdrEntryTransformerTest {

    private final EdrEntryDtoToEdrEntryTransformer transformer = new EdrEntryDtoToEdrEntryTransformer();

    private final TransformerContext context = mock(TransformerContext.class);

    @Test
    void inputOutputType() {
        assertThat(transformer.getInputType()).isNotNull();
        assertThat(transformer.getOutputType()).isNotNull();
    }

    @Test
    void verify_transform() {

        var entry = EndpointDataReferenceEntry.Builder.newInstance()
                .agreementId("aId")
                .assetId("assetId")
                .transferProcessId("tpId")
                .build();

        var dto = transformer.transform(entry, context);

        assertThat(dto).isNotNull();
        assertThat(dto.getAgreementId()).isEqualTo(entry.getAgreementId());
        assertThat(dto.getAssetId()).isEqualTo(entry.getAssetId());
        assertThat(dto.getTransferProcessId()).isEqualTo(entry.getTransferProcessId());
    }
}
