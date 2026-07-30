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

package org.eclipse.tractusx.edc.policy.cx.validator;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.tractusx.edc.policy.cx.validator.jsonschema.CxJsonSchemaPolicyDefinitionValidator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

class CxJsonSchemaPolicyDefinitionValidatorTest {

    private final CxJsonSchemaPolicyDefinitionValidator validator = new  CxJsonSchemaPolicyDefinitionValidator();

    @Test
    void shouldValidateJsonSchema_whenValidPolicyDefinition() {
        var policy = Json.createObjectBuilder()
                .add(TYPE, "Set")
                .add(ID, "id")
                .build();
        var policyDefinition = policyDefinition(policy);

        var result = validator.validate(policyDefinition);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void shouldResolveReferencedSchemas_whenPolicyContainsConstraints() {
        var constraint = Json.createObjectBuilder()
                        .add("leftOperand", "Membership")
                        .add("operator", "eq")
                        .add("rightOperand", "active");
        var permission = Json.createObjectBuilder()
                       .add("action", "access")
                        .add("constraint", Json.createArrayBuilder().add(constraint));
        var policy = Json.createObjectBuilder()
                       .add(TYPE, "Set")
                        .add(ID, "id")
                        .add("permission", Json.createArrayBuilder().add(permission))
                        .build();
        var policyDefinition = policyDefinition(policy);

        assertThatNoException().isThrownBy(() -> validator.validate(policyDefinition));
    }

    @Test
    void shouldReturnFailure_whenPolicyMissing() {
        var policyDefinition = policyDefinition(null);

        var result = validator.validate(policyDefinition);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).anyMatch(message -> message.contains("Attribute 'policy' is missing from PolicyDefinition."));
    }

    @Test
    void shouldReturnFailure_whenPolicyNotAnObject() {
        var policyDefinition = policyDefinition(Json.createArrayBuilder().build());

        var result = validator.validate(policyDefinition);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).anyMatch(message -> message.contains("Attribute 'policy' is not a valid JSON object."));
    }

    private JsonObject policyDefinition(JsonValue policy) {
        var builder =  Json.createObjectBuilder()
                .add(TYPE, "PolicyDefinition");
        if (policy != null) {
            builder.add("policy", policy);
        }

        return builder.build();
    }
}
