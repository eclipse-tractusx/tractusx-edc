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

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.tractusx.edc.api.cp.adapter.dto.EndpointDataReferenceEntryDto;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.tractusx.edc.api.cp.adapter.dto.EndpointDataReferenceEntryDto.EDR_ENTRY_DTO_AGREEMENT_ID;
import static org.eclipse.tractusx.edc.api.cp.adapter.dto.EndpointDataReferenceEntryDto.EDR_ENTRY_DTO_ASSET_ID;
import static org.eclipse.tractusx.edc.api.cp.adapter.dto.EndpointDataReferenceEntryDto.EDR_ENTRY_DTO_TYPE;
import static org.eclipse.tractusx.edc.api.cp.adapter.dto.EndpointDataReferenceEntryDto.EDR_ENTRY_TRANSFER_PROCESS_ID;


public class JsonObjectFromEndpointDataReferenceEntryDtoTransformer extends AbstractJsonLdTransformer<EndpointDataReferenceEntryDto, JsonObject> {

    public JsonObjectFromEndpointDataReferenceEntryDtoTransformer() {
        super(EndpointDataReferenceEntryDto.class, JsonObject.class);
    }

    @Override
    public @Nullable JsonObject transform(@NotNull EndpointDataReferenceEntryDto dto, @NotNull TransformerContext context) {
        return Json.createObjectBuilder()
                .add(TYPE, EDR_ENTRY_DTO_TYPE)
                .add(EDR_ENTRY_DTO_AGREEMENT_ID, dto.getAgreementId())
                .add(EDR_ENTRY_TRANSFER_PROCESS_ID, dto.getTransferProcessId())
                .add(EDR_ENTRY_DTO_ASSET_ID, dto.getAssetId())
                .build();
    }


}
