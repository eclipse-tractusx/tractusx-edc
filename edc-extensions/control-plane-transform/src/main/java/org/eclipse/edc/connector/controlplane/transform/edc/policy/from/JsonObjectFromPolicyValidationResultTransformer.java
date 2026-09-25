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

package org.eclipse.edc.connector.controlplane.transform.edc.policy.from;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyValidationResult;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

public class JsonObjectFromPolicyValidationResultTransformer extends AbstractJsonLdTransformer<PolicyValidationResult, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromPolicyValidationResultTransformer(JsonBuilderFactory jsonFactory) {
        super(PolicyValidationResult.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull PolicyValidationResult input, @NotNull TransformerContext context) {
        var objectBuilder = jsonFactory.createObjectBuilder();
        objectBuilder.add(TYPE, PolicyValidationResult.EDC_POLICY_VALIDATION_RESULT_TYPE);
        objectBuilder.add(PolicyValidationResult.EDC_POLICY_VALIDATION_RESULT_IS_VALID, input.isValid());
        objectBuilder.add(PolicyValidationResult.EDC_POLICY_VALIDATION_RESULT_ERRORS, jsonFactory.createArrayBuilder(input.errors()));
        return objectBuilder.build();
    }
}
