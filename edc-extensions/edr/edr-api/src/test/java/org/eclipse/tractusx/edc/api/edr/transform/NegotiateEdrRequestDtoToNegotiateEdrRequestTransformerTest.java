/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
 ********************************************************************************/

package org.eclipse.tractusx.edc.api.edr.transform;

import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.tractusx.edc.api.edr.dto.NegotiateEdrRequestDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.api.edr.TestFunctions.createContractOffer;
import static org.eclipse.tractusx.edc.api.edr.TestFunctions.createOffer;
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
                .counterPartyId("connectorId")
                .connectorAddress("address")
                .protocol("protocol")
                .providerId("test-provider")
                .contractOffer(createContractOffer())
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
    void verify_transformDeprecatedOffer() {
        var callback = CallbackAddress.Builder.newInstance()
                .uri("local://test")
                .build();
        var dto = NegotiateEdrRequestDto.Builder.newInstance()
                .counterPartyId("connectorId")
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
    void verify_transform_withNoProviderId() {
        var dto = NegotiateEdrRequestDto.Builder.newInstance()
                .counterPartyId("connectorId")
                .connectorAddress("address")
                .protocol("protocol")
                // do not set provider ID
                .contractOffer(createContractOffer())
                .build();

        var request = transformer.transform(dto, context);

        assertThat(request).isNotNull();
    }

    @Test
    void verify_transform_withNoConsumerId() {
        var dto = NegotiateEdrRequestDto.Builder.newInstance()
                .counterPartyId("connectorId")
                .connectorAddress("address")
                .protocol("protocol")
                // do not set consumer ID
                .providerId("urn:connector:test-provider")
                .contractOffer(createContractOffer())
                .build();

        var request = transformer.transform(dto, context);
        assertThat(request).isNotNull();
    }
}
