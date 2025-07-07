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

public class PolicyHelperFunctions {

    public static final String TX_NAMESPACE = "https://w3id.org/tractusx/v0.0.1/ns/";
    private static final String ODRL_JSONLD = "http://www.w3.org/ns/odrl.jsonld";
    private static final String BUSINESS_PARTNER_EVALUATION_KEY = "BusinessPartnerNumber";

    public static final String BUSINESS_PARTNER_LEGACY_EVALUATION_KEY = TX_NAMESPACE + BUSINESS_PARTNER_EVALUATION_KEY;

    private static final String BUSINESS_PARTNER_CONSTRAINT_KEY = TX_NAMESPACE + "BusinessPartnerGroup";

    private static final ObjectMapper MAPPER = JacksonJsonLd.createObjectMapper();

    public static JsonObject bpnGroupPolicy(Operator operator, String... allowedGroups) {
        return bpnGroupPolicy(operator.getOdrlRepresentation(), allowedGroups);
    }

    /**
     * Creates a {@link PolicyDefinition} using the given ID, that contains equality constraints for each of the given BusinessPartnerNumbers:
     * each BPN is converted into an {@link AtomicConstraint} {@code BusinessPartnerNumber EQ [BPN]}.
     */
    public static JsonObject frameworkPolicy(String id, Map<String, String> permissions) {
        return policyDefinitionBuilder(frameworkPolicy(permissions))
                .add(ID, id)
                .build();
    }

    public static JsonObject frameworkPolicy(Map<String, String> permissions) {
        return Json.createObjectBuilder()
                .add(CONTEXT, ODRL_JSONLD)
                .add(TYPE, "Set")
                .add("permission", Json.createArrayBuilder()
                        .add(frameworkPermission(permissions)))
                .build();
    }

    public static JsonObject frameworkPolicy(String leftOperand, Operator operator, Object rightOperand) {
        var constraint = atomicConstraint(leftOperand, operator.getOdrlRepresentation(), rightOperand);

        var permission = Json.createObjectBuilder()
                .add("action", "use")
                .add("constraint", Json.createObjectBuilder()
                        .add(TYPE, ODRL_LOGICAL_CONSTRAINT_TYPE)
                        .add("or", constraint)
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


    private static JsonObject bpnGroupPolicy(String operator, String... allowedGroups) {

        var groupConstraint = atomicConstraint(BUSINESS_PARTNER_CONSTRAINT_KEY, operator, Arrays.asList(allowedGroups));

        var permission = Json.createObjectBuilder()
                .add("action", "use")
                .add("constraint", Json.createObjectBuilder()
                        .add(TYPE, ODRL_LOGICAL_CONSTRAINT_TYPE)
                        .add("or", groupConstraint)
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
                .map(bpn -> atomicConstraint(TX_NAMESPACE + BUSINESS_PARTNER_EVALUATION_KEY, "eq", bpn))
                .collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add);

        return Json.createObjectBuilder()
                .add("action", "use")
                .add("constraint", Json.createObjectBuilder()
                        .add(TYPE, ODRL_LOGICAL_CONSTRAINT_TYPE)
                        .add("or", bpnConstraints)
                        .build())
                .build();
    }

    private static JsonObject frameworkPermission(Map<String, String> permissions) {

        var constraints = permissions.entrySet().stream()
                .map(permission -> atomicConstraint(permission.getKey(), "eq", permission.getValue()))
                .collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add);

        return Json.createObjectBuilder()
                .add("action", "use")
                .add("constraint", Json.createObjectBuilder()
                        .add(TYPE, ODRL_LOGICAL_CONSTRAINT_TYPE)
                        .add("or", constraints)
                        .build())
                .build();
    }

    private static JsonObject atomicConstraint(String leftOperand, String operator, Object rightOperand) {
        var builder = Json.createObjectBuilder()
                .add(TYPE, ODRL_CONSTRAINT_TYPE)
                .add("leftOperand", leftOperand)
                .add("operator", operator);

        if (rightOperand instanceof Collection<?> coll) {
            builder.add("rightOperand", coll.stream().map(Object::toString).collect(Collectors.joining(",")));
        } else {
            builder.add("rightOperand", rightOperand.toString());
        }
        return builder.build();
    }
}
