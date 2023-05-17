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

import org.eclipse.edc.api.transformer.DtoTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.tractusx.edc.api.cp.adapter.dto.EndpointDataReferenceEntryDto;
import org.eclipse.tractusx.edc.edr.spi.EndpointDataReferenceEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class EdrEntryDtoToEdrEntryTransformer implements DtoTransformer<EndpointDataReferenceEntry, EndpointDataReferenceEntryDto> {

    @Override
    public Class<EndpointDataReferenceEntryDto> getOutputType() {
        return EndpointDataReferenceEntryDto.class;
    }

    @Override
    public Class<EndpointDataReferenceEntry> getInputType() {
        return EndpointDataReferenceEntry.class;
    }

    @Override
    public @Nullable EndpointDataReferenceEntryDto transform(@NotNull EndpointDataReferenceEntry dto, @NotNull TransformerContext context) {
        return EndpointDataReferenceEntryDto.Builder.newInstance()
                .agreementId(dto.getAgreementId())
                .assetId(dto.getAssetId())
                .transferProcessId(dto.getTransferProcessId())
                .build();
    }
}
