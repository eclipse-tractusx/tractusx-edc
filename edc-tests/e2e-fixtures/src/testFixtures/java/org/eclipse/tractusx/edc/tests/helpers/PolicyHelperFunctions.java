/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 * Copyright (c) 2025 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
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

package org.eclipse.tractusx.edc.tests.helpers;


import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Operator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_CONSTRAINT_TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_LOGICAL_CONSTRAINT_TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.CX_POLICY_2025_09_NS;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.CX_POLICY_NS;

public class PolicyHelperFunctions {

    private static final String ODRL_JSONLD = "http://www.w3.org/ns/odrl.jsonld";
    private static final String BUSINESS_PARTNER_EVALUATION_KEY = "BusinessPartnerNumber";

    public static final String BUSINESS_PARTNER_LEGACY_EVALUATION_KEY = CX_POLICY_NS + BUSINESS_PARTNER_EVALUATION_KEY;

    private static final String BUSINESS_PARTNER_CONSTRAINT_KEY = CX_POLICY_2025_09_NS + "BusinessPartnerGroup";

    private static final ObjectMapper MAPPER = JacksonJsonLd.createObjectMapper();

    public static JsonObject bpnGroupPolicy(Operator operator, String... allowedGroups) {
        return bpnGroupPolicy(operator.getOdrlRepresentation(), allowedGroups);
    }

    /**
     * Creates a {@link PolicyDefinition} using the given ID, that contains equality constraints for each of the given BusinessPartnerNumbers:
     * each BPN is converted into an {@link AtomicConstraint} {@code BusinessPartnerNumber EQ [BPN]}.
     */

    public static JsonObject frameworkPolicy(String id, Map<String, String> permissions, String action) {
        return policyDefinitionBuilder(frameworkPolicy(permissions, action))
                .add(ID, id)
                .build();
    }

    public static JsonObject frameworkPolicy(Map<String, String> permissions, String action) {
        return Json.createObjectBuilder()
                .add(CONTEXT, ODRL_JSONLD)
                .add(TYPE, "Set")
                .add("permission", Json.createArrayBuilder()
                        .add(frameworkPermission(permissions, action)))
                .build();
    }

    public static JsonObject emptyPolicy() {
        return Json.createObjectBuilder()
                .add(CONTEXT, ODRL_JSONLD)
                .add(TYPE, "Set")
                .build();
    }

    public static JsonObject policyWithEmptyRule(String action) {
        var rule = Json.createObjectBuilder()
                .add("action", action)
                .build();
        var rulesArrayBuilder = Json.createArrayBuilder();
        rulesArrayBuilder.add(rule);
        return Json.createObjectBuilder()
                .add(CONTEXT, ODRL_JSONLD)
                .add(TYPE, "Set")
                .add("permission", rulesArrayBuilder)
                .build();
    }

    public static JsonObject policyFromRules(String ruleType, JsonObject... rules) {
        var rulesArrayBuilder = Json.createArrayBuilder();
        for (JsonObject rule : rules) {
            rulesArrayBuilder.add(rule);
        }
        return Json.createObjectBuilder()
                .add(CONTEXT, ODRL_JSONLD)
                .add(TYPE, "Set")
                .add(ruleType, rulesArrayBuilder)
                .build();
    }

    public static JsonObject frameworkPolicy(String leftOperand, Operator operator, Object rightOperand, String action) {
        return frameworkPolicy(leftOperand, operator, rightOperand, action, false);
    }

    public static JsonObject frameworkPolicy(String leftOperand, Operator operator, Object rightOperand, String action, boolean createRightOperandsAsArray) {
        var constraint = atomicConstraint(leftOperand, operator.getOdrlRepresentation(), rightOperand, createRightOperandsAsArray);

        var permission = Json.createObjectBuilder()
                .add("action", action)
                .add("constraint", Json.createObjectBuilder()
                        .add(TYPE, ODRL_LOGICAL_CONSTRAINT_TYPE)
                        .add("and", constraint)
                        .build())
                .build();

        return Json.createObjectBuilder()
                .add(CONTEXT, ODRL_JSONLD)
                .add(TYPE, "Set")
                .add("permission", Json.createArrayBuilder().add(permission))
                .build();
    }

    /**
     * Creates a {@link PolicyDefinition} using the given ID, that contains equality constraints for each of the given BusinessPartnerNumbers:
     * each BPN is converted into an {@link AtomicConstraint} {@code BusinessPartnerNumber EQ [BPN]}.
     */
    public static JsonObject frameworkTemplatePolicy(String id, String frameworkKind) {
        var template = fetchFrameworkTemplate().replace("${POLICY_ID}", id).replace("${FRAMEWORK_CREDENTIAL}", frameworkKind);
        try {
            return MAPPER.readValue(template, JsonObject.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonObjectBuilder policyDefinitionBuilder() {
        return Json.createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "PolicyDefinitionDto");
    }

    public static JsonObjectBuilder policyDefinitionBuilder(JsonObject policy) {
        return policyDefinitionBuilder()
                .add(EDC_NAMESPACE + "policy", policy);
    }

    public static JsonObject bpnPolicy(String... bnps) {
        return Json.createObjectBuilder()
                .add(CONTEXT, ODRL_JSONLD)
                .add(TYPE, "Set")
                .add("permission", Json.createArrayBuilder()
                        .add(permission(bnps)))
                .build();
    }

    public static JsonObject bpnPolicy(Operator operator, String... bpns) {
        JsonArrayBuilder bpnArray = Json.createArrayBuilder();
        Stream.of(bpns).forEach(bpnArray::add);

        var bpnConstraint = Json.createObjectBuilder()
                .add(TYPE, ODRL_CONSTRAINT_TYPE)
                .add("leftOperand", CX_POLICY_2025_09_NS + BUSINESS_PARTNER_EVALUATION_KEY)
                .add("operator", operator.getOdrlRepresentation())
                .add("rightOperand", bpnArray)
                .build();

        var permission = Json.createObjectBuilder()
                .add("action", CX_POLICY_2025_09_NS + "access")
                .add("constraint", Json.createObjectBuilder()
                        .add(TYPE, ODRL_LOGICAL_CONSTRAINT_TYPE)
                        .add("and", bpnConstraint)
                        .build())
                .build();
        return Json.createObjectBuilder()
                .add(CONTEXT, ODRL_JSONLD)
                .add(TYPE, "Set")
                .add("permission", Json.createArrayBuilder()
                        .add(permission))
                .build();
    }

    private static JsonObject bpnGroupPolicy(String operator, String... allowedGroups) {

        var groupConstraint = atomicConstraint(BUSINESS_PARTNER_CONSTRAINT_KEY, operator, Arrays.asList(allowedGroups), false);

        var permission = Json.createObjectBuilder()
                .add("action", CX_POLICY_2025_09_NS + "access")
                .add("constraint", Json.createObjectBuilder()
                        .add(TYPE, ODRL_LOGICAL_CONSTRAINT_TYPE)
                        .add("and", groupConstraint)
                        .build())
                .build();

        return Json.createObjectBuilder()
                .add(CONTEXT, ODRL_JSONLD)
                .add(TYPE, "Set")
                .add("permission", permission)
                .build();
    }

    private static String fetchFrameworkTemplate() {
        try (var stream = PolicyHelperFunctions.class.getClassLoader().getResourceAsStream("framework-policy.json")) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static JsonObject permission(String... bpns) {

        var bpnConstraints = Stream.of(bpns)
                .map(bpn -> atomicConstraint(CX_POLICY_2025_09_NS + BUSINESS_PARTNER_EVALUATION_KEY, "isAnyOf", bpn, false))
                .collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add);

        return Json.createObjectBuilder()
                .add("action", CX_POLICY_2025_09_NS + "access")
                .add("constraint", Json.createObjectBuilder()
                        .add(TYPE, ODRL_LOGICAL_CONSTRAINT_TYPE)
                        .add("and", bpnConstraints)
                        .build())
                .build();
    }

    public static JsonObject frameworkPermission(Map<String, String> permissions, String action) {

        var constraints = permissions.entrySet().stream()
                .map(permission -> atomicConstraint(permission.getKey(), "eq", permission.getValue(), false))
                .collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add);

        return Json.createObjectBuilder()
                .add("action", action)
                .add("constraint", Json.createObjectBuilder()
                        .add(TYPE, ODRL_LOGICAL_CONSTRAINT_TYPE)
                        .add("and", constraints)
                        .build())
                .build();
    }

    private static JsonObject atomicConstraint(String leftOperand, String operator, Object rightOperand, boolean createRightOperandsAsArray) {
        var builder = Json.createObjectBuilder()
                .add(TYPE, ODRL_CONSTRAINT_TYPE)
                .add("leftOperand", leftOperand)
                .add("operator", operator);

        if (rightOperand instanceof Collection<?> coll && createRightOperandsAsArray) {
            builder.add("rightOperand", coll.stream()
                    .map(Object::toString)
                    .collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add)
                    .build());
        } else if (rightOperand instanceof Collection<?> coll) {
            builder.add("rightOperand", coll.stream().map(Object::toString).collect(Collectors.joining(",")));
        } else {
            builder.add("rightOperand", rightOperand.toString());
        }
        return builder.build();
    }
}
