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

import jakarta.json.Json;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyBuilderFixturesV4.atomicConstraint;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyBuilderFixturesV4.logicalConstraint;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyBuilderFixturesV4.policy;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyBuilderFixturesV4.rule;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyBuilderFixturesV4.ruleWithoutActionType;

class CxJsonSchemaPolicyValidatorTest {

    private static final String ACTION = "action";
    private static final String PERMISSION = "permission";
    private static final String OBLIGATION = "obligation";
    private static final String CONSTRAINT = "constraint";
    private static final String AND = "and";
    private static final String OR = "or";
    private static final String SET = "Set";

    private static final String ACCESS = "access";
    private static final String USE = "use";

    private static final String MEMBERSHIP = "Membership";
    private static final String USAGE_PURPOSE = "UsagePurpose";
    private static final String FRAMEWORK_AGREEMENT = "FrameworkAgreement";
    private static final String WARRANTY_DEFINITION = "WarrantyDefinition";
    private static final String WARRANTY_DURATION_MONTHS = "WarrantyDurationMonths";
    private static final String DATA_PROVISIONING_END_DURATION_DAYS = "DataProvisioningEndDurationDays";
    private static final String DATA_PROVISIONING_END_DURATION_DATE = "DataProvisioningEndDate";

    private final CxJsonSchemaPolicyValidator validator = new CxJsonSchemaPolicyValidator();

    @Test
    void shouldReturnSuccess_whenValidAccessPolicy() {
        var constraint = atomicConstraint(MEMBERSHIP, "eq", "active");
        var permission = rule(ACCESS, constraint);
        var policy = policy(PERMISSION, permission);

        var result = validator.validate(policy);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldReturnSuccess_whenValidAccessPolicyWithLogicalConstraint() {
        var constraint = atomicConstraint(MEMBERSHIP, "eq", "active");
        var logicalConstraint = logicalConstraint(AND, constraint);
        var permission = rule(ACCESS, logicalConstraint);
        var policy = policy(PERMISSION, permission);

        var result = validator.validate(policy);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldReturnSuccess_whenValidUsagePolicy() {
        var usagePurposeConstraint = atomicConstraint(USAGE_PURPOSE, "isAnyOf", List.of("cx.pcf.base:1"));
        var frameworkAgreementConstraint = atomicConstraint(FRAMEWORK_AGREEMENT, "eq", "DataExchangeGovernance:1.0");
        var logicalConstraint = logicalConstraint(AND, usagePurposeConstraint, frameworkAgreementConstraint);
        var permission = rule(USE, logicalConstraint);
        var policy = policy(PERMISSION, permission);

        var result = validator.validate(policy);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldReturnFailure_whenInvalidAction() {
        var constraint = atomicConstraint(USAGE_PURPOSE, "isAnyOf", List.of("cx.pcf.base:1"));
        var permission = rule("unknown", constraint);
        var policy = policy(PERMISSION, permission);

        var result = validator.validate(policy);

        assertThat(result).isFailed();
    }

    @Test
    void shouldReturnFailure_whenActionMissing() {
        var constraint = atomicConstraint(USAGE_PURPOSE, "isAnyOf", List.of("cx.pcf.base:1"));
        var permission = ruleWithoutActionType(constraint);
        var policy = policy(PERMISSION, permission);

        var result = validator.validate(policy);

        assertThat(result).isFailed();
    }

    @Test
    void shouldReturnFailure_whenPolicyContainsEmptyRules() {
        var policy = Json.createObjectBuilder()
                .add(ID, UUID.randomUUID().toString())
                .add(TYPE, Json.createArrayBuilder().add(SET))
                .add(PERMISSION, Json.createArrayBuilder())
                .build();

        var result = validator.validate(policy);

        assertThat(result).isFailed();
    }

    @Test
    void shouldReturnFailure_whenRulesContainDifferentActions() {
        var usageConstraint = atomicConstraint(USAGE_PURPOSE, "isAnyOf", List.of("cx.pcf.base:1"));
        var usagePermission = rule(USE, usageConstraint);
        var accessConstraint = atomicConstraint(MEMBERSHIP, "eq", "active");
        var accessPermission = rule(ACCESS, accessConstraint);
        var policy = policy(PERMISSION, usagePermission, accessPermission);

        var result = validator.validate(policy);

        assertThat(result).isFailed();
    }

    @Test
    void shouldReturnFailure_whenPermissionContainsMutuallyExclusiveConstraints() {
        var usagePermission = rule(USE,
                atomicConstraint(WARRANTY_DURATION_MONTHS, "eq", "12"),
                atomicConstraint(WARRANTY_DEFINITION, "eq", "cx.warranty.contractEndDate:1")
        );
        var policy = policy(PERMISSION, usagePermission);

        var result = validator.validate(policy);

        assertThat(result).isFailed();
    }

    @Test
    void shouldReturnFailure_whenObligationContainsMutuallyExclusiveConstraints() {
        var usagePermission = rule(USE,
                atomicConstraint(WARRANTY_DURATION_MONTHS, "eq", "12")
        );

        var usageObligation = rule(USE,
                atomicConstraint(DATA_PROVISIONING_END_DURATION_DAYS, "eq", "14"),
                atomicConstraint(DATA_PROVISIONING_END_DURATION_DATE, "eq", "2026-07-31T15:35:12Z")
        );
        var policy = Json.createObjectBuilder()
                .add(ID, UUID.randomUUID().toString())
                .add(TYPE, Json.createArrayBuilder().add(SET))
                .add(PERMISSION, Json.createArrayBuilder().add(usagePermission))
                .add(OBLIGATION, Json.createArrayBuilder().add(usageObligation))
                .build();

        var result = validator.validate(policy);

        assertThat(result).isFailed();
    }

    @Test
    void shouldReturnFailure_whenInvalidLogicalConstraint() {
        var logicalConstraint = logicalConstraint(OR,
                atomicConstraint(USAGE_PURPOSE, "isAnyOf", List.of("cx.pcf.base:1")),
                atomicConstraint(FRAMEWORK_AGREEMENT, "eq", "DataExchangeGovernance:1.0")
        );
        var permission = rule(USE, logicalConstraint);
        var policy = policy(PERMISSION, permission);

        var result = validator.validate(policy);

        assertThat(result).isFailed();
    }

    @Test
    void shouldReturnFailure_whenRuleHasMultipleActions() {
        var action = Json.createArrayBuilder()
                .add(ACCESS)
                .add(USE)
                .build();

        var permission = Json.createObjectBuilder()
                .add(ACTION, action)
                .add(CONSTRAINT, Json.createArrayBuilder()
                        .add(atomicConstraint(MEMBERSHIP, "eq", "active")))
                .build();

        var policy = Json.createObjectBuilder()
                .add(ID, UUID.randomUUID().toString())
                .add(TYPE, Json.createArrayBuilder().add(SET))
                .add(PERMISSION, Json.createArrayBuilder().add(permission))
                .build();

        var result = validator.validate(policy);

        assertThat(result).isFailed();
    }
}
