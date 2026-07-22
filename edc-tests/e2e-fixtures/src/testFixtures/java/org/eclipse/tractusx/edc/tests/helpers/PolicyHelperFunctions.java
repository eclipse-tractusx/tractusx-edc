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

package org.eclipse.tractusx.edc.tests.helpers;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import org.eclipse.edc.policy.model.Operator;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.eclipse.edc.connector.controlplane.test.system.utils.PolicyFixtures.policy;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.tractusx.edc.cx.CxJsonLdExtension.CX_POLICY_2025_09_CONTEXT;

public class PolicyHelperFunctions {

    public static final String ODRL_CONTEXT = "https://w3id.org/dspace/2025/1/odrl-profile.jsonld";

    private static final String FRAMEWORK_AGREEMENT_KEY = "FrameworkAgreement";
    private static final String USAGE_PURPOSE_KEY = "UsagePurpose";

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
                            .add("and", constraints)
                            .build())
                        .build())
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

    public static JsonObject inForceDatePolicyLegacy(String operatorStart, Object startDate, String operatorEnd, Object endDate) {
        var constraint = Json.createObjectBuilder()
                .add("@type", "LogicalConstraint")
                .add("and", Json.createArrayBuilder()
                        .add(atomicConstraint("https://w3id.org/edc/v0.0.1/ns/inForceDate", operatorStart, startDate, false))
                        .add(atomicConstraint("https://w3id.org/edc/v0.0.1/ns/inForceDate", operatorEnd, endDate, false))
                        .add(atomicConstraint("https://w3id.org/catenax/policy/Membership", "eq", "active", false))
                        .build())
                .build();

        return policy(List.of(Json.createObjectBuilder()
                .add("action", "use")
                .add("constraint", constraint)
                .build()));
    }

    private static String operatorValueWithoutNamespace(Operator operator) {
        var parts = operator.getOdrlRepresentation().split("/");
        return parts[parts.length - 1];
    }
}
