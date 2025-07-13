package org.eclipse.tractusx.edc.policy.cx.validator;

import jakarta.json.JsonObject;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryObject;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryValue;
import org.eclipse.edc.validator.jsonobject.validators.OptionalIdNotBlank;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.*;

public class AtomicConstraintValidator implements Validator<JsonObject> {
    private final JsonLdPath path;
    private final String policyType;
    private final String ruleType;

    private AtomicConstraintValidator(JsonLdPath path, String policyType, String ruleType) {
        this.path = path;
        this.policyType = policyType;
        this.ruleType = ruleType;
    }

    public static AtomicConstraintValidator instance(JsonLdPath path, String policyType, String ruleType) {
        return new AtomicConstraintValidator(path, policyType, ruleType);
    }

    @Override
    public ValidationResult validate(JsonObject input) {
        return JsonObjectValidator.newValidator()
                .verify(ODRL_LEFT_OPERAND_ATTRIBUTE, MandatoryObject::new)
                .verifyObject(ODRL_LEFT_OPERAND_ATTRIBUTE, b -> LeftOperandValidator.instance(b, policyType, ruleType))
                .verify(ODRL_OPERATOR_ATTRIBUTE, MandatoryObject::new)
                .verifyObject(ODRL_OPERATOR_ATTRIBUTE, b -> b.verifyId(OptionalIdNotBlank::new))
                .verify(ODRL_RIGHT_OPERAND_ATTRIBUTE, MandatoryValue::new)
                .build()
                .validate(input);
    }
}