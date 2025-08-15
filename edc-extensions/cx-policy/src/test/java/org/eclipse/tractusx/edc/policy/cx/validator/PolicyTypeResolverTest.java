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
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_ACTION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OBLIGATION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PERMISSION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PROHIBITION_ATTRIBUTE;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.ACTION_ACCESS;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.ACTION_USAGE;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PolicyTypeResolverTest {

    @Test
    void shouldReturnAccessPolicyType_whenNoPolicyRulesPresent() {
        JsonObject policy = Json.createObjectBuilder().build();

        String result = PolicyTypeResolver.resolve(policy);

        assertThat(result).isEqualTo(ACTION_ACCESS);
    }

    @Test
    void shouldReturnActionFromPermission_whenPermissionHasStringAction() {
        JsonObject permission = Json.createObjectBuilder()
                .add(ODRL_ACTION_ATTRIBUTE, ACTION_USAGE)
                .build();
        JsonObject policy = Json.createObjectBuilder()
                .add(ODRL_PERMISSION_ATTRIBUTE, Json.createArrayBuilder().add(permission))
                .build();

        String result = PolicyTypeResolver.resolve(policy);

        assertThat(result).isEqualTo(ACTION_USAGE);
    }

    @Test
    void shouldReturnActionFromPermission_whenPermissionHasObjectAction() {
        JsonObject actionObj = Json.createObjectBuilder()
                .add("@id", ACTION_USAGE)
                .build();
        JsonObject permission = Json.createObjectBuilder()
                .add(ODRL_ACTION_ATTRIBUTE, actionObj)
                .build();
        JsonObject policy = Json.createObjectBuilder()
                .add(ODRL_PERMISSION_ATTRIBUTE, Json.createArrayBuilder().add(permission))
                .build();

        String result = PolicyTypeResolver.resolve(policy);

        assertThat(result).isEqualTo(ACTION_USAGE);
    }

    @Test
    void shouldReturnActionFromPermission_whenPermissionHasArrayAction() {
        JsonObject actionObj = Json.createObjectBuilder()
                .add("@id", ACTION_USAGE)
                .build();
        JsonArrayBuilder actionArray = Json.createArrayBuilder().add(actionObj);
        JsonObject permission = Json.createObjectBuilder()
                .add(ODRL_ACTION_ATTRIBUTE, actionArray)
                .build();
        JsonObject policy = Json.createObjectBuilder()
                .add(ODRL_PERMISSION_ATTRIBUTE, Json.createArrayBuilder().add(permission))
                .build();

        String result = PolicyTypeResolver.resolve(policy);

        assertThat(result).isEqualTo(ACTION_USAGE);
    }

    @Test
    void shouldReturnActionFromObligation_whenNoPermissionsButObligationsPresent() {
        JsonObject obligation = Json.createObjectBuilder()
                .add(ODRL_ACTION_ATTRIBUTE, ACTION_USAGE)
                .build();
        JsonObject policy = Json.createObjectBuilder()
                .add(ODRL_OBLIGATION_ATTRIBUTE, Json.createArrayBuilder().add(obligation))
                .build();

        String result = PolicyTypeResolver.resolve(policy);

        assertThat(result).isEqualTo(ACTION_USAGE);
    }

    @Test
    void shouldReturnActionFromProhibition_whenNoPermissionsOrObligationsPresent() {
        JsonObject prohibition = Json.createObjectBuilder()
                .add(ODRL_ACTION_ATTRIBUTE, ACTION_USAGE)
                .build();
        JsonObject policy = Json.createObjectBuilder()
                .add(ODRL_PROHIBITION_ATTRIBUTE, Json.createArrayBuilder().add(prohibition))
                .build();

        String result = PolicyTypeResolver.resolve(policy);

        assertThat(result).isEqualTo(ACTION_USAGE);
    }

    @Test
    void shouldThrowException_whenRuleHasNoAction() {
        JsonObject permission = Json.createObjectBuilder()
                .add("other", "value")
                .build();
        JsonObject policy = Json.createObjectBuilder()
                .add(ODRL_PERMISSION_ATTRIBUTE, Json.createArrayBuilder().add(permission))
                .build();

        Exception ex = assertThrows(IllegalArgumentException.class, () -> PolicyTypeResolver.resolve(policy));
        assertThat(ex.getMessage()).contains("Rule does not contain any action field");
    }

    @Test
    void shouldReturnAccessPolicyType_whenPermissionArrayIsEmpty() {
        JsonObject policy = Json.createObjectBuilder()
                .add(ODRL_PERMISSION_ATTRIBUTE, Json.createArrayBuilder())
                .build();

        String result = PolicyTypeResolver.resolve(policy);

        assertThat(result).isEqualTo(ACTION_ACCESS);
    }

    @Test
    void shouldReturnAccessPolicyType_whenObligationArrayIsEmpty() {
        JsonObject policy = Json.createObjectBuilder()
                .add(ODRL_OBLIGATION_ATTRIBUTE, Json.createArrayBuilder())
                .build();

        String result = PolicyTypeResolver.resolve(policy);

        assertThat(result).isEqualTo(ACTION_ACCESS);
    }

    @Test
    void shouldReturnAccessPolicyType_whenProhibitionArrayIsEmpty() {
        JsonObject policy = Json.createObjectBuilder()
                .add(ODRL_PROHIBITION_ATTRIBUTE, Json.createArrayBuilder())
                .build();

        String result = PolicyTypeResolver.resolve(policy);

        assertThat(result).isEqualTo(ACTION_ACCESS);
    }

    @Test
    void shouldReturnEmptyString_whenActionObjectMissingId() {
        JsonObject actionObj = Json.createObjectBuilder()
                .add("type", "action")
                .build();
        JsonObject permission = Json.createObjectBuilder()
                .add(ODRL_ACTION_ATTRIBUTE, actionObj)
                .build();
        JsonObject policy = Json.createObjectBuilder()
                .add(ODRL_PERMISSION_ATTRIBUTE, Json.createArrayBuilder().add(permission))
                .build();

        Exception ex = assertThrows(IllegalArgumentException.class, () -> PolicyTypeResolver.resolve(policy));
        assertThat(ex.getMessage()).contains("Rule does not contain any action field");
    }

    @Test
    void shouldThrowException_whenRuleHasInvalidAction() {
        JsonObject permission = Json.createObjectBuilder()
                .add(ODRL_ACTION_ATTRIBUTE, "invalid-action")
                .build();
        JsonObject policy = Json.createObjectBuilder()
                .add(ODRL_PERMISSION_ATTRIBUTE, Json.createArrayBuilder().add(permission))
                .build();

        Exception ex = assertThrows(IllegalArgumentException.class, () -> PolicyTypeResolver.resolve(policy));
        assertThat(ex.getMessage()).contains("Rule does not contain a valid policy type");
    }

    @Test
    void shouldThrowException_whenRuleHasInconsistentActions() {
        JsonObject permission = Json.createObjectBuilder()
                .add(ODRL_ACTION_ATTRIBUTE, ACTION_ACCESS)
                .build();
        JsonObject prohibition = Json.createObjectBuilder()
                .add(ODRL_ACTION_ATTRIBUTE, ACTION_USAGE)
                .build();
        JsonObject policy = Json.createObjectBuilder()
                .add(ODRL_PERMISSION_ATTRIBUTE, Json.createArrayBuilder().add(permission))
                .add(ODRL_PROHIBITION_ATTRIBUTE, Json.createArrayBuilder().add(prohibition))
                .build();

        Exception ex = assertThrows(IllegalArgumentException.class, () -> PolicyTypeResolver.resolve(policy));
        assertThat(ex.getMessage()).contains("Policy contains inconsistent policy types");
    }
}
