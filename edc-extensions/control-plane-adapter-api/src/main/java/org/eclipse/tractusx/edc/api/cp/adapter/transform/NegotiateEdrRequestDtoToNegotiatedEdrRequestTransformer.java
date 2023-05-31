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
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.tractusx.edc.api.cp.adapter.dto.NegotiateEdrRequestDto;
import org.eclipse.tractusx.edc.spi.cp.adapter.model.NegotiateEdrRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NegotiateEdrRequestDtoToNegotiatedEdrRequestTransformer implements DtoTransformer<NegotiateEdrRequestDto, NegotiateEdrRequest> {

    @Override
    public Class<NegotiateEdrRequestDto> getInputType() {
        return NegotiateEdrRequestDto.class;
    }

    @Override
    public Class<NegotiateEdrRequest> getOutputType() {
        return NegotiateEdrRequest.class;
    }

    @Override
    public @Nullable NegotiateEdrRequest transform(@NotNull NegotiateEdrRequestDto object, @NotNull TransformerContext context) {
        var contractOffer = ContractOffer.Builder.newInstance()
                .id(object.getOffer().getOfferId())
                .assetId(object.getOffer().getAssetId())
                .providerId(getId(object.getProviderId(), object.getConnectorAddress()))
                .policy(object.getOffer().getPolicy())
                .build();

        return NegotiateEdrRequest.Builder.newInstance()
                .connectorId(object.getConnectorId())
                .connectorAddress(object.getConnectorAddress())
                .protocol(object.getProtocol())
                .offer(contractOffer)
                .callbackAddresses(object.getCallbackAddresses())
                .build();
    }

    private String getId(String value, String defaultValue) {
        return value != null ? value : defaultValue;
    }

}
