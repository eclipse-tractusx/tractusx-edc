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

package org.eclipse.edc.connector.controlplane.transform.edc.contractdefinition.from;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_ACCESSPOLICY_ID;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_ASSETS_SELECTOR;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_CONTRACTPOLICY_ID;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

public class JsonObjectFromContractDefinitionTransformer extends AbstractJsonLdTransformer<ContractDefinition, JsonObject> {
    private final TypeManager typeManager;
    private final String typeContext;
    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromContractDefinitionTransformer(JsonBuilderFactory jsonFactory, TypeManager typeManager, String typeContext) {
        super(ContractDefinition.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
        this.typeManager = typeManager;
        this.typeContext = typeContext;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull ContractDefinition contractDefinition, @NotNull TransformerContext context) {
        var criteria = contractDefinition.getAssetsSelector().stream()
                .map(criterion -> context.transform(criterion, JsonObject.class))
                .collect(toJsonArray());

        var builder = jsonFactory.createObjectBuilder()
                .add(ID, contractDefinition.getId())
                .add(TYPE, CONTRACT_DEFINITION_TYPE)
                .add(CONTRACT_DEFINITION_ACCESSPOLICY_ID, contractDefinition.getAccessPolicyId())
                .add(CONTRACT_DEFINITION_CONTRACTPOLICY_ID, contractDefinition.getContractPolicyId())
                .add(CONTRACT_DEFINITION_ASSETS_SELECTOR, criteria);

        if (!contractDefinition.getPrivateProperties().isEmpty()) {
            var privatePropBuilder = jsonFactory.createObjectBuilder();
            transformProperties(contractDefinition.getPrivateProperties(), privatePropBuilder, typeManager.getMapper(typeContext), context);
            builder.add(ContractDefinition.CONTRACT_DEFINITION_PRIVATE_PROPERTIES, privatePropBuilder);
        }

        return builder.build();
    }
}
