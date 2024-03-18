/*
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft
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
 */

package org.eclipse.tractusx.edc.api.edr.v2.transform;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractOfferDescription;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.tractusx.edc.api.edr.v2.dto.NegotiateEdrRequestDto;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;


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
            case NegotiateEdrRequestDto.EDR_REQUEST_DTO_COUNTERPARTY_ADDRESS:
                transformString(value, builder::connectorAddress, context);
                break;
            case NegotiateEdrRequestDto.EDR_REQUEST_DTO_PROTOCOL:
                transformString(value, builder::protocol, context);
                break;
            case NegotiateEdrRequestDto.EDR_REQUEST_DTO_COUNTERPARTY_ID:
                transformString(value, builder::counterPartyId, context);
                break;
            case NegotiateEdrRequestDto.EDR_REQUEST_DTO_PROVIDER_ID:
                transformString(value, builder::providerId, context);
                break;
            case NegotiateEdrRequestDto.EDR_REQUEST_DTO_CALLBACK_ADDRESSES:
                var addresses = new ArrayList<CallbackAddress>();
                transformArrayOrObject(value, CallbackAddress.class, addresses::add, context);
                builder.callbackAddresses(addresses);
                break;
            case NegotiateEdrRequestDto.EDR_REQUEST_DTO_OFFER:
                transformArrayOrObject(value, ContractOfferDescription.class, builder::offer, context);
                break;
            default:
                context.problem()
                        .unexpectedType()
                        .type(NegotiateEdrRequestDto.EDR_REQUEST_DTO_TYPE)
                        .property(key)
                        .actual(key)
                        .expected(NegotiateEdrRequestDto.EDR_REQUEST_DTO_COUNTERPARTY_ADDRESS)
                        .expected(NegotiateEdrRequestDto.EDR_REQUEST_DTO_PROTOCOL)
                        .expected(NegotiateEdrRequestDto.EDR_REQUEST_DTO_COUNTERPARTY_ID)
                        .expected(NegotiateEdrRequestDto.EDR_REQUEST_DTO_PROVIDER_ID)
                        .expected(NegotiateEdrRequestDto.EDR_REQUEST_DTO_CALLBACK_ADDRESSES)
                        .expected(NegotiateEdrRequestDto.EDR_REQUEST_DTO_OFFER)
                        .report();
                break;
        }
    }
}
