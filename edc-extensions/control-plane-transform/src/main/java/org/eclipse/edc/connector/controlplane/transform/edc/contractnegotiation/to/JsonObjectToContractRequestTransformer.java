/********************************************************************************
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.controlplane.transform.edc.contractnegotiation.to;

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest.CALLBACK_ADDRESSES;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest.CONTRACT_REQUEST_COUNTER_PARTY_ADDRESS;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest.POLICY;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest.PROTOCOL;

public class JsonObjectToContractRequestTransformer extends AbstractJsonLdTransformer<JsonObject, ContractRequest> {

    public JsonObjectToContractRequestTransformer() {
        super(JsonObject.class, ContractRequest.class);
    }

    @Override
    public @Nullable ContractRequest transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var contractRequestBuilder = ContractRequest.Builder.newInstance()
                .counterPartyAddress(transformString(jsonObject.get(CONTRACT_REQUEST_COUNTER_PARTY_ADDRESS), context))
                .protocol(transformString(jsonObject.get(PROTOCOL), context));

        contractRequestBuilder.contractOffer(contractOffer(jsonObject, context));

        var callbackAddress = jsonObject.get(CALLBACK_ADDRESSES);
        if (callbackAddress != null) {
            contractRequestBuilder.callbackAddresses(transformArray(callbackAddress, CallbackAddress.class, context));
        }

        return contractRequestBuilder.build();
    }

    private ContractOffer contractOffer(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var policy = jsonObject.get(POLICY);
        if (policy != null) {
            return transformObject(policy, ContractOffer.class, context);
        }

        return null;
    }

}
