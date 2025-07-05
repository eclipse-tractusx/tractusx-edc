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
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.ACTION_ACCESS;

/**
 * Validates access policy constraints according to the specification.
 * Ensures that access policies contain only permissions with valid constraints,
 * while obligations and prohibitions must be empty.
 */
public class AccessPolicyValidator implements Validator<JsonObject> {
    private final JsonLdPath path;

    public AccessPolicyValidator(JsonLdPath path) {
        this.path = path;
    }

    @Override
    public ValidationResult validate(JsonObject input) {
        return JsonObjectValidator.newValidator()
                .verifyArrayItem(ODRL_PERMISSION_ATTRIBUTE, AccessPermissionValidator::instance)
                .verify(ODRL_OBLIGATION_ATTRIBUTE, ArrayIsEmpty::new)
                .verify(ODRL_PROHIBITION_ATTRIBUTE, ArrayIsEmpty::new)
                .build()
                .validate(input);
    }

    private static final class AccessPermissionValidator {
        public static JsonObjectValidator.Builder instance(JsonObjectValidator.Builder builder) {
            return builder
                    .verify(path -> ActionTypeIs.orAbsent(path, ACTION_ACCESS))
                    .verifyArrayItem(ODRL_CONSTRAINT_ATTRIBUTE, b -> ConstraintValidator.instance(b, ACTION_ACCESS, ODRL_PERMISSION_ATTRIBUTE));
        }
    }
}
