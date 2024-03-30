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

import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.edc.transform.spi.TypeTransformer;
import org.eclipse.tractusx.edc.api.edr.v2.dto.NegotiateEdrRequestDto;
import org.eclipse.tractusx.edc.edr.spi.types.NegotiateEdrRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NegotiateEdrRequestDtoToNegotiatedEdrRequestTransformer implements TypeTransformer<NegotiateEdrRequestDto, NegotiateEdrRequest> {

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
                .policy(object.getOffer().getPolicy())
                .build();

        return NegotiateEdrRequest.Builder.newInstance()
                .connectorId(object.getCounterPartyId())
                .connectorAddress(object.getCounterPartyAddress())
                .protocol(object.getProtocol())
                .offer(contractOffer)
                .callbackAddresses(object.getCallbackAddresses())
                .build();
    }

    private String getId(String value, String defaultValue) {
        return value != null ? value : defaultValue;
    }

}
