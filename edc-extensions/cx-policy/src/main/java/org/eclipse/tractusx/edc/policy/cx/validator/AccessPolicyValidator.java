package org.eclipse.tractusx.edc.policy.cx.validator;

import jakarta.json.JsonObject;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.*;
import static org.eclipse.tractusx.edc.policy.cx.validator.AllowedConstraints.ACCESS_POLICY_TYPE;

public class AccessPolicyValidator implements Validator<JsonObject> {


    private final JsonLdPath path;

    public AccessPolicyValidator(JsonLdPath path) {
        this.path = path;
    }


    @Override
    public ValidationResult validate(JsonObject input) {
        return JsonObjectValidator.newValidator()
                .verifyArrayItem(ODRL_PERMISSION_ATTRIBUTE, AccessPermissionValidator::instance)
                .verifyArrayItem(ODRL_OBLIGATION_ATTRIBUTE, AccessDutyValidator::instance)
                .verifyArrayItem(ODRL_PROHIBITION_ATTRIBUTE, AccessProhibitionValidator::instance)
                .build()
                .validate(input);
    }

    private static final class AccessPermissionValidator {
        public static JsonObjectValidator.Builder instance(JsonObjectValidator.Builder builder) {
            return builder
                    .verify(path -> new ActionTypeIs(path, ACCESS_POLICY_TYPE))
                    .verifyArrayItem(ODRL_CONSTRAINT_ATTRIBUTE, b -> ConstraintValidator.instance(b, ACCESS_POLICY_TYPE));
        }
    }
    private static class AccessProhibitionValidator {
        public static JsonObjectValidator.Builder instance(JsonObjectValidator.Builder builder) {

            return builder
                    .verify(path -> new ArrayIsEmpty(path, ODRL_PROHIBITION_ATTRIBUTE));
        }

    }

    private static class AccessDutyValidator {
        public static JsonObjectValidator.Builder instance(JsonObjectValidator.Builder builder) {

            return builder
                    .verify(path -> new ArrayIsEmpty(path, ODRL_DUTY_ATTRIBUTE));
        }

    }
}