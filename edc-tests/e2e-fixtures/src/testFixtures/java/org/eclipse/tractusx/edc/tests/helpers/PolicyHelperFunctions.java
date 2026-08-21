/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 * Copyright (c) 2025 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 * Copyright (c) 2026 Cofinity-X GmbH
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

package org.eclipse.tractusx.edc.tests.helpers;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Operator;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_LOGICAL_CONSTRAINT_TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.tractusx.edc.cx.CxJsonLdExtension.CX_POLICY_2025_09_CONTEXT;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.CX_POLICY_2025_09_NS;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.CX_POLICY_NS;

public class PolicyHelperFunctions {

    public static final String ODRL_CONTEXT = "https://w3id.org/dspace/2025/1/odrl-profile.jsonld";

    private static final String FRAMEWORK_AGREEMENT_KEY = "FrameworkAgreement";
    public static final String FRAMEWORK_AGREEMENT_LITERAL = CX_POLICY_2025_09_NS + "FrameworkAgreement";
    private static final String USAGE_PURPOSE_KEY = "UsagePurpose";
    private static final String USAGE_PURPOSE_LITERAL = CX_POLICY_2025_09_NS + "UsagePurpose";

    public static final String DATA_USAGE_END_DATE_KEY = "DataUsageEndDate";
    public static final String DATA_USAGE_END_DURATION_KEY = "DataUsageEndDurationDays";

    public static JsonObject emptyPolicy() {
        return Json.createObjectBuilder()
                .add(CONTEXT, Json.createArrayBuilder()
                        .add(ODRL_CONTEXT)
                        .add(CX_POLICY_2025_09_CONTEXT))
                .add(TYPE, "Set")
                .add(ID, "id")
                .build();
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
                .add(CONTEXT, Json.createArrayBuilder()
                        .add(ODRL_CONTEXT)
                        .add(CX_POLICY_2025_09_CONTEXT))
                .add(TYPE, "Set")
                .add(ID, "id")
                .add("permission", Json.createArrayBuilder()
                        .add(frameworkConstraint(new HashMap<>(permissions), action, Operator.EQ, false)))
                .build();
    }

    public static JsonObject frameworkPolicy(Map<String, String> permissions, String action, String operator) {
        return frameworkPolicy(permissions, action, Operator.valueOf(operator));
    }

    public static JsonObject frameworkPolicy(Map<String, String> permissions, String action, Operator operator) {
        return Json.createObjectBuilder()
                .add(CONTEXT, Json.createArrayBuilder()
                        .add(ODRL_CONTEXT)
                        .add(CX_POLICY_2025_09_CONTEXT))
                .add(TYPE, "Set")
                .add(ID, "id")
                .add("permission", Json.createArrayBuilder()
                        .add(frameworkConstraint(new HashMap<>(permissions), action, operator, false)))
                .build();
    }

    public static JsonObject bpnGroupPolicy(String operator, boolean rightOperandAsArray, String... allowedGroups) {

        var groupConstraint = atomicConstraint("BusinessPartnerGroup", operator, Arrays.asList(allowedGroups), rightOperandAsArray);

        var permission = Json.createObjectBuilder()
                .add("action", "access")
                .add("constraint", Json.createArrayBuilder()
                        .add(groupConstraint)
                        .build())
                .build();

        return Json.createObjectBuilder()
                .add(CONTEXT, Json.createArrayBuilder()
                        .add(ODRL_CONTEXT)
                        .add(CX_POLICY_2025_09_CONTEXT))
                .add(TYPE, "Set")
                .add(ID, "id")
                .add("permission", Json.createArrayBuilder()
                        .add(permission))
                .build();
    }

    public static JsonObject frameworkPolicy(String leftOperand, Operator operator, Object rightOperand, String action) {
        return frameworkPolicy(leftOperand, operator, rightOperand, action, false);
    }

    public static JsonObject frameworkPolicy(String leftOperand, Operator operator, Object rightOperand, String action, boolean createRightOperandsAsArray) {
        var constraint = atomicConstraint(leftOperand, operatorValueWithoutNamespace(operator), rightOperand, createRightOperandsAsArray);

        var constraintsBuilder = Json.createArrayBuilder()
                .add(constraint);

        if (!leftOperand.equals(FRAMEWORK_AGREEMENT_KEY) && action.contains("use")) {
            constraintsBuilder.add(frameworkAgreementConstraint());
        }

        if (!leftOperand.equals(USAGE_PURPOSE_KEY) && action.contains("use")) {
            constraintsBuilder.add(usagePurposeConstraint());
        }

        var permission = Json.createObjectBuilder()
                .add("action", action)
                .add("constraint", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                            .add("and", constraintsBuilder.build())
                            .build())
                        .build())
                .build();

        return Json.createObjectBuilder()
                .add(CONTEXT, Json.createArrayBuilder()
                        .add(ODRL_CONTEXT)
                        .add(CX_POLICY_2025_09_CONTEXT))
                .add(TYPE, "Set")
                .add(ID, "id")
                .add("permission", Json.createArrayBuilder().add(permission))
                .build();
    }

    private static JsonObject frameworkAgreementConstraint() {
        return Json.createObjectBuilder()
                .add("leftOperand", FRAMEWORK_AGREEMENT_KEY)
                .add("operator", "eq")
                .add("rightOperand", "DataExchangeGovernance:1.0")
                .build();
    }

    private static JsonObject usagePurposeConstraint() {
        return Json.createObjectBuilder()
                .add("leftOperand", USAGE_PURPOSE_KEY)
                .add("operator", "isAnyOf")
                .add("rightOperand",  Json.createArrayBuilder().add("cx.pcf.base:1").build())
                .build();
    }

    public static JsonObject legacyFrameworkPolicy() {
        var constraint1 = atomicConstraint(CX_POLICY_NS + "FrameworkAgreement", Operator.EQ.getOdrlRepresentation(), "DataExchangeGovernance:1.0", false);
        var constraint2 = atomicConstraint(CX_POLICY_NS + "UsagePurpose", Operator.EQ.getOdrlRepresentation(), "cx.core.digitalTwinRegistry:1", false);

        var constraintsBuilder = Json.createArrayBuilder()
                .add(constraint1)
                .add(constraint2);

        var permission = Json.createObjectBuilder()
                .add("action", "use")
                .add("constraint", Json.createObjectBuilder()
                        .add(TYPE, ODRL_LOGICAL_CONSTRAINT_TYPE)
                        .add("and", constraintsBuilder.build())
                        .build())
                .build();

        return Json.createObjectBuilder()
                .add(CONTEXT, ODRL_CONTEXT)
                .add(TYPE, "Set")
                .add("permission", Json.createArrayBuilder().add(permission))
                .build();
    }

    private static JsonObject atomicConstraint(String leftOperand, String operator, Object rightOperand, boolean createRightOperandsAsArray) {
        var builder = Json.createObjectBuilder()
                .add("leftOperand", leftOperand)
                .add("operator", operator);

        if (rightOperand instanceof Collection<?> coll && createRightOperandsAsArray) {
            builder.add("rightOperand", coll.stream()
                    .map(Object::toString)
                    .collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add)
                    .build());
        } else if (rightOperand instanceof Collection<?> coll) {
            builder.add("rightOperand", coll.stream().map(Object::toString).collect(Collectors.joining(",")));
        } else if (createRightOperandsAsArray) {
            builder.add("rightOperand", Json.createArrayBuilder().add(rightOperand.toString()).build());
        } else {
            builder.add("rightOperand", rightOperand.toString());
        }
        return builder.build();
    }

    public static JsonObject dataUsageEndDurationDays(Integer duration) {
        var constraint = Json.createObjectBuilder()
                .add("and", Json.createArrayBuilder()
                        .add(atomicConstraint(DATA_USAGE_END_DURATION_KEY, "eq", duration, false))
                        .add(frameworkAgreementConstraint())
                        .add(usagePurposeConstraint())
                        .build())
                .build();

        var permission = Json.createObjectBuilder()
                .add("action", "use")
                .add("constraint", Json.createArrayBuilder()
                        .add(constraint)
                        .build())
                .build();

        var contextArrayBuilder = Json.createArrayBuilder();
        contextArrayBuilder.add(ODRL_CONTEXT);
        contextArrayBuilder.add(CX_POLICY_2025_09_CONTEXT);

        return Json.createObjectBuilder()
                .add(CONTEXT, contextArrayBuilder)
                .add(TYPE, "Set")
                .add(ID, "id")
                .add("permission", Json.createArrayBuilder()
                        .add(permission))
                .build();
    }

    public static JsonObject bpnPolicy(String... bpns) {
        return bpnPolicy(Operator.IS_ANY_OF, bpns);
    }

    public static JsonObject dataUsageEndDate(String endDate) {
        var constraint = Json.createObjectBuilder()
                .add("and", Json.createArrayBuilder()
                        .add(atomicConstraint(DATA_USAGE_END_DATE_KEY, "eq", endDate, false))
                        .add(frameworkAgreementConstraint())
                        .add(usagePurposeConstraint())
                        .build())
                .build();

        var permission = Json.createObjectBuilder()
                .add("action", "use")
                .add("constraint", Json.createArrayBuilder()
                        .add(constraint)
                        .build())
                .build();

        var contextArrayBuilder = Json.createArrayBuilder();
        contextArrayBuilder.add(ODRL_CONTEXT);
        contextArrayBuilder.add(CX_POLICY_2025_09_CONTEXT);

        return Json.createObjectBuilder()
                .add(CONTEXT, contextArrayBuilder)
                .add(TYPE, "Set")
                .add(ID, "id")
                .add("permission", Json.createArrayBuilder()
                        .add(permission))
                .build();
    }

    public static JsonObjectBuilder policyDefinitionBuilder() {
        return Json.createObjectBuilder()
                .add(TYPE, EDC_NAMESPACE + "PolicyDefinitionDto");
    }

    public static JsonObjectBuilder policyDefinitionBuilder(JsonObject policy) {
        return policyDefinitionBuilder()
                .add(EDC_NAMESPACE + "policy", policy);
    }

    public static JsonObject bpnPolicy(Operator operator, String... bpns) {
        JsonArrayBuilder bpnArray = Json.createArrayBuilder();
        Stream.of(bpns).forEach(bpnArray::add);

        var bpnConstraint = Json.createObjectBuilder()
                .add("leftOperand", "BusinessPartnerNumber")
                .add("operator", operatorValueWithoutNamespace(operator))
                .add("rightOperand", bpnArray)
                .build();

        var permission = Json.createObjectBuilder()
                .add("action", "access")
                .add("constraint", Json.createArrayBuilder()
                        .add(bpnConstraint)
                        .build())
                .build();
        return Json.createObjectBuilder()
                .add(CONTEXT, Json.createArrayBuilder()
                        .add(ODRL_CONTEXT)
                        .add(CX_POLICY_2025_09_CONTEXT))
                .add(TYPE, "Set")
                .add(ID, "id")
                .add("permission", Json.createArrayBuilder()
                        .add(permission))
                .build();
    }

    public static JsonObject dataProvisioningEndDate(String endDate) {
        var requiredUsagePermissionConstraints = Json.createObjectBuilder()
                .add("and", Json.createArrayBuilder()
                        .add(frameworkAgreementConstraint())
                        .add(usagePurposeConstraint())
                        .build())
                .build();

        var dataProvisioningConstraint = atomicConstraint("DataProvisioningEndDate", "eq", endDate, false);

        return Json.createObjectBuilder()
                .add(CONTEXT, Json.createArrayBuilder()
                        .add(ODRL_CONTEXT)
                        .add(CX_POLICY_2025_09_CONTEXT))
                .add(TYPE, "Set")
                .add(ID, "id")
                .add("permission", Json.createArrayBuilder(
                        List.of(Json.createObjectBuilder()
                                .add("action", "use")
                                .add("constraint", Json.createArrayBuilder()
                                        .add(requiredUsagePermissionConstraints))
                                .build())
                ))
                .add("obligation", Json.createArrayBuilder(
                        List.of(Json.createObjectBuilder()
                                .add("action", "use")
                                .add("constraint", Json.createArrayBuilder()
                                        .add(dataProvisioningConstraint))
                                .build())
                )).build();
    }

    public static JsonObject frameworkConstraint(Map<String, Object> operandMappings, String action, Operator operator, boolean createRightOperandsAsArray) {
        var constraints = operandMappings.entrySet().stream()
                .map(constraint -> atomicConstraint(constraint.getKey(), operatorValueWithoutNamespace(operator), constraint.getValue(), createRightOperandsAsArray))
                .collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add);

        if (action.contains("use")) {
            if (!operandMappings.containsKey(FRAMEWORK_AGREEMENT_KEY)) {
                constraints.add(frameworkAgreementConstraint());
            }
            if (!operandMappings.containsKey(USAGE_PURPOSE_KEY)) {
                constraints.add(usagePurposeConstraint());
            }
        }

        return Json.createObjectBuilder()
                .add("action", action)
                .add("constraint", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add(TYPE, ODRL_LOGICAL_CONSTRAINT_TYPE)
                                .add("and", constraints)
                                .build())
                        .build())
                .build();
    }

    private static String operatorValueWithoutNamespace(Operator operator) {
        var parts = operator.getOdrlRepresentation().split("/");
        return parts[parts.length - 1];
    }
}
