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

import org.eclipse.edc.api.model.CallbackAddressDto;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.tractusx.edc.api.cp.adapter.dto.TransferOpenRequestDto;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.api.cp.adapter.TestFunctions.createOffer;
import static org.mockito.Mockito.mock;

public class TransferOpenRequestDtoToTransferOpenRequestTransformerTest {

    private static final String DEFAULT_CONSUMER_ID = "urn:connector:test-consumer";
    private final Instant now = Instant.now();
    private final Clock clock = Clock.fixed(now, UTC);

    private final TransferOpenRequestDtoToTransferOpenRequestTransformer transformer = new TransferOpenRequestDtoToTransferOpenRequestTransformer(clock, DEFAULT_CONSUMER_ID);

    private final TransformerContext context = mock(TransformerContext.class);

    @Test
    void inputOutputType() {
        assertThat(transformer.getInputType()).isNotNull();
        assertThat(transformer.getOutputType()).isNotNull();
    }

    @Test
    void verify_transform() {
        var callback = CallbackAddressDto.Builder.newInstance()
                .uri("local://test")
                .build();
        var dto = TransferOpenRequestDto.Builder.newInstance()
                .connectorId("connectorId")
                .connectorAddress("address")
                .protocol("protocol")
                .consumerId("test-consumer")
                .providerId("test-provider")
                .offer(createOffer("offerId", "assetId"))
                .callbackAddresses(List.of(callback))
                .build();

        var request = transformer.transform(dto, context);

        assertThat(request).isNotNull();
        assertThat(request.getConnectorId()).isEqualTo("connectorId");
        assertThat(request.getConnectorAddress()).isEqualTo("address");
        assertThat(request.getProtocol()).isEqualTo("protocol");
        assertThat(request.getOffer().getId()).isEqualTo("offerId");
        assertThat(request.getOffer().getContractStart().toInstant()).isEqualTo(clock.instant());
        assertThat(request.getOffer().getContractEnd().toInstant()).isEqualTo(clock.instant().plusSeconds(dto.getOffer().getValidity()));
        assertThat(request.getOffer().getPolicy()).isNotNull();
        assertThat(request.getCallbackAddresses()).hasSize(1);
    }

    @Test
    void verify_transfor_withNoProviderId() {
        var dto = TransferOpenRequestDto.Builder.newInstance()
                .connectorId("connectorId")
                .connectorAddress("address")
                .protocol("protocol")
                .consumerId("urn:connector:test-consumer")
                // do not set provider ID
                .offer(createOffer("offerId", "assetId"))
                .build();

        var request = transformer.transform(dto, context);

        assertThat(request).isNotNull();
        assertThat(request.getOffer().getProvider()).asString().isEqualTo(dto.getConnectorAddress());
        assertThat(request.getOffer().getConsumer()).asString().isEqualTo("urn:connector:test-consumer");
    }

    @Test
    void verify_transform_withNoConsumerId() {
        var dto = TransferOpenRequestDto.Builder.newInstance()
                .connectorId("connectorId")
                .connectorAddress("address")
                .protocol("protocol")
                // do not set consumer ID
                .providerId("urn:connector:test-provider")
                .offer(createOffer("offerId", "assetId"))
                .build();

        var request = transformer.transform(dto, context);
        assertThat(request).isNotNull();
        assertThat(request.getOffer().getProvider()).asString().isEqualTo("urn:connector:test-provider");
        assertThat(request.getOffer().getConsumer()).asString().isEqualTo(DEFAULT_CONSUMER_ID);
    }
}
