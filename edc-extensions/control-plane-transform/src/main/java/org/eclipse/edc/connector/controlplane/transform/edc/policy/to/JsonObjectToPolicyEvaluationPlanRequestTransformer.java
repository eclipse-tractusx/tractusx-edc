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

package org.eclipse.edc.connector.controlplane.transform.edc.policy.to;

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyEvaluationPlanRequest;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.connector.controlplane.policy.spi.PolicyEvaluationPlanRequest.EDC_POLICY_EVALUATION_PLAN_REQUEST_POLICY_SCOPE;

public class JsonObjectToPolicyEvaluationPlanRequestTransformer extends AbstractJsonLdTransformer<JsonObject, PolicyEvaluationPlanRequest> {

    public JsonObjectToPolicyEvaluationPlanRequestTransformer() {
        super(JsonObject.class, PolicyEvaluationPlanRequest.class);
    }

    @Override
    public @Nullable PolicyEvaluationPlanRequest transform(@NotNull JsonObject input, @NotNull TransformerContext context) {
        var policyScope = transformString(input.get(EDC_POLICY_EVALUATION_PLAN_REQUEST_POLICY_SCOPE), context);
        return new PolicyEvaluationPlanRequest(policyScope);
    }

}
