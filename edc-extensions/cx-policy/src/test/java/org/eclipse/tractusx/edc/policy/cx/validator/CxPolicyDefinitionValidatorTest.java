/********************************************************************************
 * Copyright (c) 2025 Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
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
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.junit.assertions.FailureAssert;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.junit.jupiter.api.Test;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_ACTION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_AND_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OBLIGATION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OR_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PERMISSION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_POLICY_TYPE_SET;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyBuilderFixtures.atomicConstraint;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyBuilderFixtures.logicalConstraint;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyBuilderFixtures.policy;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyBuilderFixtures.policyDefinition;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyBuilderFixtures.rule;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyBuilderFixtures.ruleWithoutActionType;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.ACTION_ACCESS;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.ACTION_USAGE;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.DATA_PROVISIONING_END_DATE_LITERAL;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.DATA_PROVISIONING_END_DURATION_LITERAL;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.FRAMEWORK_AGREEMENT_LITERAL;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.MEMBERSHIP_LITERAL;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.USAGE_PURPOSE_LITERAL;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.WARRANTY_DEFINITION_LITERAL;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.WARRANTY_DURATION_MONTHS_LITERAL;

class CxPolicyDefinitionValidatorTest {

    @Test
    void shouldReturnSuccess_whenValidAccessPolicy() {
        JsonObject constraint = atomicConstraint(MEMBERSHIP_LITERAL);
        JsonObject permission = rule(ACTION_ACCESS, constraint);
        JsonObject policy = policy(ODRL_PERMISSION_ATTRIBUTE, permission);
        JsonObject input = policyDefinition(policy, "some-id");

        ValidationResult result = CxPolicyDefinitionValidator.instance().validate(input);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldReturnSuccess_whenValidAccessPolicyWithLogicalConstraint() {
        JsonObject constraint = atomicConstraint(MEMBERSHIP_LITERAL);
        JsonObject logicalConstraint = logicalConstraint(ODRL_AND_CONSTRAINT_ATTRIBUTE, constraint);
        JsonObject permission = rule(ACTION_ACCESS, logicalConstraint);
        JsonObject policy = policy(ODRL_PERMISSION_ATTRIBUTE, permission);
        JsonObject input = policyDefinition(policy, "some-id");

        ValidationResult result = CxPolicyDefinitionValidator.instance().validate(input);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldReturnSuccess_whenValidUsagePolicy() {
        JsonObject constraint = atomicConstraint(USAGE_PURPOSE_LITERAL);
        JsonObject permission = rule(ACTION_USAGE, constraint);
        JsonObject policy = policy(ODRL_PERMISSION_ATTRIBUTE, permission);
        JsonObject input = policyDefinition(policy, "some-id");

        ValidationResult result = CxPolicyDefinitionValidator.instance().validate(input);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldReturnFailure_whenPolicyTypeUnknown() {
        JsonObject constraint = atomicConstraint(USAGE_PURPOSE_LITERAL);
        JsonObject permission = rule("Unknown-type", constraint);
        JsonObject policy = policy(ODRL_PERMISSION_ATTRIBUTE, permission);
        JsonObject input = policyDefinition(policy, "some-id");

        ValidationResult result = CxPolicyDefinitionValidator.instance().validate(input);

        assertThat(result).isFailed();
        FailureAssert.assertThat(result.getFailure()).messages().anyMatch(msg -> msg.contains("Rule does not contain a valid policy type"));
    }

    @Test
    void shouldReturnFailure_whenPolicyMissing() {
        JsonObject input = Json.createObjectBuilder()
                .add(ID, "some-id")
                .build();

        ValidationResult result = CxPolicyDefinitionValidator.instance().validate(input);

        assertThat(result).isFailed();
        FailureAssert.assertThat(result.getFailure()).messages().anyMatch(msg -> msg.contains("mandatory object '" + EDC_NAMESPACE + "policy' is missing"));
    }

    @Test
    void shouldReturnFailure_whenPolicyTypeMissing() {
        JsonObject constraint = atomicConstraint(USAGE_PURPOSE_LITERAL);
        JsonObject permission = ruleWithoutActionType(constraint);
        JsonObject policy = policy(ODRL_PERMISSION_ATTRIBUTE, permission);
        JsonObject input = policyDefinition(policy, "some-id");

        ValidationResult result = CxPolicyDefinitionValidator.instance().validate(input);

        assertThat(result).isFailed();
        FailureAssert.assertThat(result.getFailure()).messages().anyMatch(msg -> msg.contains("Rule does not contain any action field"));
    }

    @Test
    void shouldReturnSuccess_whenPolicyContainEmptyRules() {
        JsonObject policy = Json.createObjectBuilder()
                .add(TYPE, Json.createArrayBuilder().add(ODRL_POLICY_TYPE_SET))
                .add(ODRL_PERMISSION_ATTRIBUTE, Json.createArrayBuilder())
                .build();
        JsonObject input = policyDefinition(policy, "some-id");

        ValidationResult result = CxPolicyDefinitionValidator.instance().validate(input);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldReturnFailure_whenRuleContainDifferentPolicyType() {
        JsonObject usageConstraint = atomicConstraint(USAGE_PURPOSE_LITERAL);
        JsonObject usagePermission = rule(ACTION_USAGE, usageConstraint);
        JsonObject accessConstraint = atomicConstraint(MEMBERSHIP_LITERAL);
        JsonObject accessPermission = rule(ACTION_ACCESS, accessConstraint);
        JsonObject policy = policy(ODRL_PERMISSION_ATTRIBUTE, usagePermission, accessPermission);
        JsonObject input = policyDefinition(policy, "some-id");

        ValidationResult result = CxPolicyDefinitionValidator.instance().validate(input);

        assertThat(result).isFailed();
        FailureAssert.assertThat(result.getFailure()).messages().anyMatch(msg -> msg.contains("Policy contains inconsistent policy types"));
    }

    @Test
    void shouldReturnFailure_whenPermissionContainsMutuallyExclusiveConstraints() {
        JsonObject usagePermission = rule(ACTION_USAGE,
                atomicConstraint(WARRANTY_DURATION_MONTHS_LITERAL),
                atomicConstraint(WARRANTY_DEFINITION_LITERAL)
        );
        JsonObject policy = policy(ODRL_PERMISSION_ATTRIBUTE, usagePermission);
        JsonObject input = policyDefinition(policy, "some-id");

        ValidationResult result = CxPolicyDefinitionValidator.instance().validate(input);

        assertThat(result).isFailed();
        FailureAssert.assertThat(result.getFailure()).messages().anyMatch(msg ->
                msg.contains(WARRANTY_DURATION_MONTHS_LITERAL) && msg.contains(WARRANTY_DEFINITION_LITERAL) && msg.contains("is mutually exclusive"));
    }

    @Test
    void shouldReturnFailure_whenObligationContainsMutuallyExclusiveConstraints() {
        JsonObject usagePermission = rule(ACTION_USAGE,
                atomicConstraint(WARRANTY_DURATION_MONTHS_LITERAL)
        );

        JsonObject usageObligation = rule(ACTION_USAGE,
                atomicConstraint(DATA_PROVISIONING_END_DURATION_LITERAL),
                atomicConstraint(DATA_PROVISIONING_END_DATE_LITERAL)
        );
        JsonObject policy = Json.createObjectBuilder()
                .add(TYPE, Json.createArrayBuilder().add(ODRL_POLICY_TYPE_SET))
                .add(ODRL_PERMISSION_ATTRIBUTE, Json.createArrayBuilder().add(usagePermission))
                .add(ODRL_OBLIGATION_ATTRIBUTE, Json.createArrayBuilder().add(usageObligation))
                .build();

        JsonObject input = policyDefinition(policy, "some-id");

        ValidationResult result = CxPolicyDefinitionValidator.instance().validate(input);

        assertThat(result).isFailed();
        FailureAssert.assertThat(result.getFailure()).messages().anyMatch(msg ->
                msg.contains(DATA_PROVISIONING_END_DURATION_LITERAL) && msg.contains(DATA_PROVISIONING_END_DATE_LITERAL) && msg.contains("is mutually exclusive"));
    }

    @Test
    void shouldReturnFailure_whenInvalidLogicalConstraint() {
        JsonObject logicalConstraint = logicalConstraint(ODRL_OR_CONSTRAINT_ATTRIBUTE,
                atomicConstraint(MEMBERSHIP_LITERAL),
                atomicConstraint(FRAMEWORK_AGREEMENT_LITERAL)
        );
        JsonObject permission = rule(ACTION_ACCESS, logicalConstraint);
        JsonObject policy = policy(ODRL_PERMISSION_ATTRIBUTE, permission);
        JsonObject input = policyDefinition(policy, "some-id");

        ValidationResult result = CxPolicyDefinitionValidator.instance().validate(input);

        assertThat(result).isFailed();
        FailureAssert.assertThat(result.getFailure()).messages().anyMatch(msg -> msg.contains("Policy includes not allowed logical constraints"));
    }

    @Test
    void shouldThrowException_whenRuleHasMultipleActions() {
        JsonArray action = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add(ID, ACTION_ACCESS))
                .add(Json.createObjectBuilder().add(ID, ACTION_USAGE))
                .build();

        JsonObject permission = Json.createObjectBuilder()
                .add(ODRL_ACTION_ATTRIBUTE, action)
                .add(ODRL_CONSTRAINT_ATTRIBUTE, Json.createArrayBuilder().add(atomicConstraint(MEMBERSHIP_LITERAL)))
                .build();

        JsonObject policy = Json.createObjectBuilder()
                .add(TYPE, Json.createArrayBuilder().add(ODRL_POLICY_TYPE_SET))
                .add(ODRL_PERMISSION_ATTRIBUTE, Json.createArrayBuilder().add(permission))
                .build();
        JsonObject input = policyDefinition(policy, "some-id");

        ValidationResult result = CxPolicyDefinitionValidator.instance().validate(input);

        assertThat(result).isFailed();
        FailureAssert.assertThat(result.getFailure()).messages().anyMatch(msg -> msg.contains("Action array should contain only one action object"));
    }
}
