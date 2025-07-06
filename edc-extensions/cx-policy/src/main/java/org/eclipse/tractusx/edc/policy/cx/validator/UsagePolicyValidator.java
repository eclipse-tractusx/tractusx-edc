package org.eclipse.tractusx.edc.policy.cx.validator;

import jakarta.json.JsonObject;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OBLIGATION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PERMISSION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PROHIBITION_ATTRIBUTE;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.USAGE_POLICY_TYPE;

public class UsagePolicyValidator implements Validator<JsonObject> {
    private final JsonLdPath path;

    public UsagePolicyValidator(JsonLdPath path) {
        this.path = path;
    }


    @Override
    public ValidationResult validate(JsonObject input) {
        return JsonObjectValidator.newValidator()
                .verifyArrayItem(ODRL_PERMISSION_ATTRIBUTE, UsagePermissionValidator::instance)
                .verifyArrayItem(ODRL_OBLIGATION_ATTRIBUTE, UsageDutyValidator::instance)
                .verifyArrayItem(ODRL_PROHIBITION_ATTRIBUTE, UsageProhibitionValidator::instance)
                .build()
                .validate(input);
    }

    private static final class UsagePermissionValidator {
        public static JsonObjectValidator.Builder instance(JsonObjectValidator.Builder builder) {
            return builder
                    .verify(path -> new ActionTypeIs(path, USAGE_POLICY_TYPE))
                    .verifyArrayItem(ODRL_CONSTRAINT_ATTRIBUTE, b -> ConstraintValidator.instance(b, USAGE_POLICY_TYPE));
        }
    }

    private static final class UsageProhibitionValidator {
        public static JsonObjectValidator.Builder instance(JsonObjectValidator.Builder builder) {
            return builder
                    .verify(path -> new ActionTypeIs(path, USAGE_POLICY_TYPE))
                    .verifyArrayItem(ODRL_CONSTRAINT_ATTRIBUTE, b -> ConstraintValidator.instance(b, USAGE_POLICY_TYPE));
        }
    }

    private static final class UsageDutyValidator {
        public static JsonObjectValidator.Builder instance(JsonObjectValidator.Builder builder) {
            return builder
                    .verify(path -> new ActionTypeIs(path, USAGE_POLICY_TYPE))
                    .verifyArrayItem(ODRL_CONSTRAINT_ATTRIBUTE, b -> ConstraintValidator.instance(b, USAGE_POLICY_TYPE));
        }
    }
}
