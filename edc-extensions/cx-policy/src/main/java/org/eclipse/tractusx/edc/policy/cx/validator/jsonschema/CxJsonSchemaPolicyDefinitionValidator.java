/********************************************************************************
 * Copyright (c) 2026 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
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

package org.eclipse.tractusx.edc.policy.cx.validator.jsonschema;

import jakarta.json.JsonObject;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

public class CxJsonSchemaPolicyDefinitionValidator implements Validator<JsonObject> {

    private static final String POLICY_ATTRIBUTE_NAME = "policy";

    private final CxJsonSchemaPolicyValidator policyValidator;

    public CxJsonSchemaPolicyDefinitionValidator() {
        this.policyValidator = new CxJsonSchemaPolicyValidator();
    }

    @Override
    public ValidationResult validate(JsonObject input) {
        var policy = input.getJsonObject(POLICY_ATTRIBUTE_NAME);
        return policyValidator.validate(policy);
    }
}
