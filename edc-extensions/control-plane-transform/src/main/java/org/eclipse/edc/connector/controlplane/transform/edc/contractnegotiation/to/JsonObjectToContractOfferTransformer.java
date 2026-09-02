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
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JsonObjectToContractOfferTransformer extends AbstractJsonLdTransformer<JsonObject, ContractOffer> {

    public JsonObjectToContractOfferTransformer() {
        super(JsonObject.class, ContractOffer.class);
    }

    @Override
    public @Nullable ContractOffer transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var policy = context.transform(jsonObject, Policy.class);
        if (policy == null) {
            return null;
        }
        var id = nodeId(jsonObject);
        return ContractOffer.Builder.newInstance()
                .id(id)
                .assetId(policy.getTarget())
                .policy(policy)
                .build();
    }
}
