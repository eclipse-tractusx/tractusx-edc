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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.dialect.Dialects;
import com.networknt.schema.resource.IriResourceLoader;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;
import org.eclipse.edc.validator.spi.Violation;

import java.util.HashMap;
import java.util.Map;

public class CxJsonSchemaPolicyValidator implements Validator<JsonObject> {
    private static final String CX_POLICY_SCHEMA_PREFIX = "https://w3id.org/catenax/2025/9/policy";
    private static final String CX_POLICY_SCHEMA_LOCATION = "classpath:schema/cx-policy";

    private static final String DSPACE_2025_SCHEMA_PREFIX = "https://w3id.org/dspace/2025/1/negotiation";
    private static final String DSPACE_2025_SCHEMA_LOCATION = "classpath:schema/dspace";

    private static final String CX_POLICY_SCHEMA = CX_POLICY_SCHEMA_PREFIX + "/policy-schema.json";

    private final SchemaRegistry schemaRegistry;
    private final ObjectMapper objectMapper;

    private final Map<String, String> prefixMappings = new HashMap<>() {
        {
            put(CX_POLICY_SCHEMA_PREFIX + "/schema", CX_POLICY_SCHEMA_LOCATION);
            put(CX_POLICY_SCHEMA_PREFIX, CX_POLICY_SCHEMA_LOCATION);
            put(DSPACE_2025_SCHEMA_PREFIX, DSPACE_2025_SCHEMA_LOCATION);
        }
    };

    public CxJsonSchemaPolicyValidator() {
        this.objectMapper = JacksonJsonLd.createObjectMapper();
        this.schemaRegistry = SchemaRegistry.withDialect(Dialects.getDraft201909(), builder -> builder
                .schemaIdResolvers(schemaIdResolvers -> prefixMappings.forEach(schemaIdResolvers::mapPrefix))
                .resourceLoaders(resourceLoaders -> resourceLoaders.add(IriResourceLoader.getInstance())));
    }

    @Override
    public ValidationResult validate(JsonObject input) {
        var schemaValidator = schemaRegistry.getSchema(SchemaLocation.of(CX_POLICY_SCHEMA));
        var node = objectMapper.convertValue(input, JsonNode.class);
        var response = schemaValidator.validate(node);
        if (response.isEmpty()) {
            return ValidationResult.success();
        }

        var violations = response.stream()
                .map(error -> Violation.violation(error.getMessage(), error.getInstanceLocation().toString()))
                .toList();

        return ValidationResult.failure(violations);
    }
}
