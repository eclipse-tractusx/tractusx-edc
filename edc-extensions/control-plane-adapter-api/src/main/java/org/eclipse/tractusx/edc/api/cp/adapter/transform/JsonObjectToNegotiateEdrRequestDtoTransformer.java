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

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.api.model.CallbackAddressDto;
import org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractOfferDescription;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.tractusx.edc.api.cp.adapter.dto.NegotiateEdrRequestDto;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

import static org.eclipse.tractusx.edc.api.cp.adapter.dto.NegotiateEdrRequestDto.CALLBACK_ADDRESSES;
import static org.eclipse.tractusx.edc.api.cp.adapter.dto.NegotiateEdrRequestDto.CONNECTOR_ADDRESS;
import static org.eclipse.tractusx.edc.api.cp.adapter.dto.NegotiateEdrRequestDto.CONNECTOR_ID;
import static org.eclipse.tractusx.edc.api.cp.adapter.dto.NegotiateEdrRequestDto.OFFER;
import static org.eclipse.tractusx.edc.api.cp.adapter.dto.NegotiateEdrRequestDto.PROTOCOL;
import static org.eclipse.tractusx.edc.api.cp.adapter.dto.NegotiateEdrRequestDto.PROVIDER_ID;


public class JsonObjectToNegotiateEdrRequestDtoTransformer extends AbstractJsonLdTransformer<JsonObject, NegotiateEdrRequestDto> {

    public JsonObjectToNegotiateEdrRequestDtoTransformer() {
        super(JsonObject.class, NegotiateEdrRequestDto.class);
    }

    @Override
    public @Nullable NegotiateEdrRequestDto transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var builder = NegotiateEdrRequestDto.Builder.newInstance();

        visitProperties(jsonObject, (k, v) -> setProperties(k, v, builder, context));
        return builder.build();
    }

    private void setProperties(String key, JsonValue value, NegotiateEdrRequestDto.Builder builder, TransformerContext context) {
        switch (key) {
            case CONNECTOR_ADDRESS:
                transformString(value, builder::connectorAddress, context);
                break;
            case PROTOCOL:
                transformString(value, builder::protocol, context);
                break;
            case CONNECTOR_ID:
                transformString(value, builder::connectorId, context);
                break;
            case PROVIDER_ID:
                transformString(value, builder::providerId, context);
                break;
            case CALLBACK_ADDRESSES:
                var addresses = new ArrayList<CallbackAddressDto>();
                transformArrayOrObject(value, CallbackAddressDto.class, addresses::add, context);
                builder.callbackAddresses(addresses);
                break;
            case OFFER:
                transformArrayOrObject(value, ContractOfferDescription.class, builder::offer, context);
                break;
            default:
                context.problem()
                        .unexpectedType()
                        .type(NegotiateEdrRequestDto.TYPE)
                        .property(key)
                        .actual(key)
                        .expected(CONNECTOR_ADDRESS)
                        .expected(PROTOCOL)
                        .expected(CONNECTOR_ID)
                        .expected(PROVIDER_ID)
                        .expected(CALLBACK_ADDRESSES)
                        .expected(OFFER)
                        .report();
                break;
        }
    }
}
