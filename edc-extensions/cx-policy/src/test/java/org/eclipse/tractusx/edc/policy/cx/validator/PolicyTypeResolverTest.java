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
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.ACCESS_POLICY_TYPE;

class PolicyTypeResolverTest {

    @Test
    void shouldReturnAccessPolicyType_whenNoPolicyRulesPresent() {
        JsonObject policy = Json.createObjectBuilder().build();

        String result = PolicyTypeResolver.resolve(policy);

        assertThat(result).isEqualTo(ACCESS_POLICY_TYPE);
    }

    @Test
    void shouldReturnActionFromPermission_whenPermissionHasStringAction() {
        JsonObject permission = Json.createObjectBuilder()
                .add(ODRL_ACTION_ATTRIBUTE, "odrl:use")
                .build();
        JsonObject policy = Json.createObjectBuilder()
                .add(ODRL_PERMISSION_ATTRIBUTE, Json.createArrayBuilder().add(permission))
                .build();

        String result = PolicyTypeResolver.resolve(policy);

        assertThat(result).isEqualTo("odrl:use");
    }

    @Test
    void shouldReturnActionFromPermission_whenPermissionHasObjectAction() {
        JsonObject actionObj = Json.createObjectBuilder()
                .add("@id", "odrl:use")
                .build();
        JsonObject permission = Json.createObjectBuilder()
                .add(ODRL_ACTION_ATTRIBUTE, actionObj)
                .build();
        JsonObject policy = Json.createObjectBuilder()
                .add(ODRL_PERMISSION_ATTRIBUTE, Json.createArrayBuilder().add(permission))
                .build();

        String result = PolicyTypeResolver.resolve(policy);

        assertThat(result).isEqualTo("odrl:use");
    }

    @Test
    void shouldReturnActionFromPermission_whenPermissionHasArrayAction() {
        JsonObject actionObj = Json.createObjectBuilder()
                .add("@id", "odrl:use")
                .build();
        JsonArrayBuilder actionArray = Json.createArrayBuilder().add(actionObj);
        JsonObject permission = Json.createObjectBuilder()
                .add(ODRL_ACTION_ATTRIBUTE, actionArray)
                .build();
        JsonObject policy = Json.createObjectBuilder()
                .add(ODRL_PERMISSION_ATTRIBUTE, Json.createArrayBuilder().add(permission))
                .build();

        String result = PolicyTypeResolver.resolve(policy);

        assertThat(result).isEqualTo("odrl:use");
    }

    @Test
    void shouldReturnActionFromObligation_whenNoPermissionsButObligationsPresent() {
        JsonObject obligation = Json.createObjectBuilder()
                .add(ODRL_ACTION_ATTRIBUTE, "odrl:compensate")
                .build();
        JsonObject policy = Json.createObjectBuilder()
                .add(ODRL_OBLIGATION_ATTRIBUTE, Json.createArrayBuilder().add(obligation))
                .build();

        String result = PolicyTypeResolver.resolve(policy);

        assertThat(result).isEqualTo("odrl:compensate");
    }

    @Test
    void shouldReturnActionFromProhibition_whenNoPermissionsOrObligationsPresent() {
        JsonObject prohibition = Json.createObjectBuilder()
                .add(ODRL_ACTION_ATTRIBUTE, "odrl:distribute")
                .build();
        JsonObject policy = Json.createObjectBuilder()
                .add(ODRL_PROHIBITION_ATTRIBUTE, Json.createArrayBuilder().add(prohibition))
                .build();

        String result = PolicyTypeResolver.resolve(policy);

        assertThat(result).isEqualTo("odrl:distribute");
    }

    @Test
    void shouldReturnEmptyString_whenRuleHasNoAction() {
        JsonObject permission = Json.createObjectBuilder()
                .add("other", "value")
                .build();
        JsonObject policy = Json.createObjectBuilder()
                .add(ODRL_PERMISSION_ATTRIBUTE, Json.createArrayBuilder().add(permission))
                .build();

        String result = PolicyTypeResolver.resolve(policy);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnAccessPolicyType_whenPermissionArrayIsEmpty() {
        JsonObject policy = Json.createObjectBuilder()
                .add(ODRL_PERMISSION_ATTRIBUTE, Json.createArrayBuilder())
                .build();

        String result = PolicyTypeResolver.resolve(policy);

        assertThat(result).isEqualTo(ACCESS_POLICY_TYPE);
    }

    @Test
    void shouldReturnAccessPolicyType_whenObligationArrayIsEmpty() {
        JsonObject policy = Json.createObjectBuilder()
                .add(ODRL_OBLIGATION_ATTRIBUTE, Json.createArrayBuilder())
                .build();

        String result = PolicyTypeResolver.resolve(policy);

        assertThat(result).isEqualTo(ACCESS_POLICY_TYPE);
    }

    @Test
    void shouldReturnAccessPolicyType_whenProhibitionArrayIsEmpty() {
        JsonObject policy = Json.createObjectBuilder()
                .add(ODRL_PROHIBITION_ATTRIBUTE, Json.createArrayBuilder())
                .build();

        String result = PolicyTypeResolver.resolve(policy);

        assertThat(result).isEqualTo(ACCESS_POLICY_TYPE);
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

        String result = PolicyTypeResolver.resolve(policy);

        assertThat(result).isEmpty();
    }

}