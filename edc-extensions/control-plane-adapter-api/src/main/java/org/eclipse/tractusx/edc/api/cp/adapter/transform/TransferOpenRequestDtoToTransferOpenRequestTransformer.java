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
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.tractusx.edc.api.cp.adapter.dto.TransferOpenRequestDto;
import org.eclipse.tractusx.edc.spi.cp.adapter.types.TransferOpenRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.stream.Collectors;

public class TransferOpenRequestDtoToTransferOpenRequestTransformer implements DtoTransformer<TransferOpenRequestDto, TransferOpenRequest> {

    private final Clock clock;
    private final String defaultConsumerId;

    public TransferOpenRequestDtoToTransferOpenRequestTransformer(Clock clock, String defaultConsumerId) {
        this.clock = clock;
        this.defaultConsumerId = defaultConsumerId;
    }

    @Override
    public Class<TransferOpenRequestDto> getInputType() {
        return TransferOpenRequestDto.class;
    }

    @Override
    public Class<TransferOpenRequest> getOutputType() {
        return TransferOpenRequest.class;
    }

    @Override
    public @Nullable TransferOpenRequest transform(@NotNull TransferOpenRequestDto object, @NotNull TransformerContext context) {
        var callbacks = object.getCallbackAddresses().stream().map(c -> context.transform(c, CallbackAddress.class)).collect(Collectors.toList());
        var now = ZonedDateTime.ofInstant(clock.instant(), clock.getZone());

        var contractOffer = ContractOffer.Builder.newInstance()
                .id(object.getOffer().getOfferId())
                .asset(Asset.Builder.newInstance().id(object.getOffer().getAssetId()).build())
                .consumer(createUri(object.getConsumerId(), defaultConsumerId))
                .provider(createUri(object.getProviderId(), object.getConnectorAddress()))
                .policy(object.getOffer().getPolicy())
                .contractStart(now)
                .contractEnd(now.plusSeconds(object.getOffer().getValidity()))
                .build();

        return TransferOpenRequest.Builder.newInstance()
                .connectorId(object.getConnectorId())
                .connectorAddress(object.getConnectorAddress())
                .consumerId(object.getConsumerId())
                .providerId(object.getProviderId())
                .protocol(object.getProtocol())
                .offer(contractOffer)
                .callbackAddresses(callbacks)
                .build();
    }

    private URI createUri(String value, String defaultValue) {
        return URI.create(value != null ? value : defaultValue);
    }
}
