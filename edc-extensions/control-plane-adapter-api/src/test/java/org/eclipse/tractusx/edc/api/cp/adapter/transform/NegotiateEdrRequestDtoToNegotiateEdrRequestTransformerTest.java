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

import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.tractusx.edc.api.cp.adapter.dto.NegotiateEdrRequestDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.api.cp.adapter.TestFunctions.createOffer;
import static org.mockito.Mockito.mock;

public class NegotiateEdrRequestDtoToNegotiateEdrRequestTransformerTest {

    private final NegotiateEdrRequestDtoToNegotiatedEdrRequestTransformer transformer = new NegotiateEdrRequestDtoToNegotiatedEdrRequestTransformer();

    private final TransformerContext context = mock(TransformerContext.class);

    @Test
    void inputOutputType() {
        assertThat(transformer.getInputType()).isNotNull();
        assertThat(transformer.getOutputType()).isNotNull();
    }

    @Test
    void verify_transform() {
        var callback = CallbackAddress.Builder.newInstance()
                .uri("local://test")
                .build();
        var dto = NegotiateEdrRequestDto.Builder.newInstance()
                .connectorId("connectorId")
                .connectorAddress("address")
                .protocol("protocol")
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
        assertThat(request.getOffer().getPolicy()).isNotNull();
        assertThat(request.getCallbackAddresses()).hasSize(1);
    }

    @Test
    void verify_transfor_withNoProviderId() {
        var dto = NegotiateEdrRequestDto.Builder.newInstance()
                .connectorId("connectorId")
                .connectorAddress("address")
                .protocol("protocol")
                // do not set provider ID
                .offer(createOffer("offerId", "assetId"))
                .build();

        var request = transformer.transform(dto, context);

        assertThat(request).isNotNull();
        assertThat(request.getOffer().getProviderId()).asString().isEqualTo(dto.getConnectorAddress());
    }

    @Test
    void verify_transform_withNoConsumerId() {
        var dto = NegotiateEdrRequestDto.Builder.newInstance()
                .connectorId("connectorId")
                .connectorAddress("address")
                .protocol("protocol")
                // do not set consumer ID
                .providerId("urn:connector:test-provider")
                .offer(createOffer("offerId", "assetId"))
                .build();

        var request = transformer.transform(dto, context);
        assertThat(request).isNotNull();
        assertThat(request.getOffer().getProviderId()).asString().isEqualTo("urn:connector:test-provider");
    }
}
