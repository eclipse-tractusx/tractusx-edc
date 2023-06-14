/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.policy.cx.common;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintFunction;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Collectors;

import static jakarta.json.JsonValue.ValueType.ARRAY;
import static jakarta.json.JsonValue.ValueType.OBJECT;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

/**
 * Base processing for constraint functions that verify a permission against a Catena-X verifiable presentation.
 */
public abstract class AbstractVpConstraintFunction implements AtomicConstraintFunction<Permission> {
    protected static final String CREDENTIAL_SUBJECT = PolicyNamespaces.W3C_VC_PREFIX + "#credentialSubject";

    protected static final String VALUE = "@value";

    protected final String errorPrefix;

    protected final String credentialType;

    private static final String ERROR_PREFIX_TEMPLATE = "Invalid %s VC format: ";

    /**
     * Ctor.
     *
     * @param credentialType the credential type that will be verified against.
     */
    public AbstractVpConstraintFunction(String credentialType) {
        requireNonNull(credentialType);
        this.credentialType = credentialType;
        this.errorPrefix = format(ERROR_PREFIX_TEMPLATE, credentialType);
    }

    /**
     * Validates the operator is in the set of expected operators.
     */
    protected boolean validateOperator(Operator operator, PolicyContext context, Operator... expectedOperators) {
        var set = stream(expectedOperators).collect(Collectors.toSet());
        if (!set.contains(operator)) {
            var valid = set.stream().map(Enum::toString).collect(joining(","));
            context.reportProblem(format("Unsupported operator for %s credential constraint, only %s allowed: %s", credentialType, valid, operator));
            return false;
        }
        return true;
    }

    /**
     * Validates the VP by checking that it is a {@link JsonObject}.
     */
    protected boolean validatePresentation(@Nullable Object vp, PolicyContext context) {
        if (vp == null) {
            context.reportProblem(format("%s VP not found", credentialType));
            return false;
        }

        if (!(vp instanceof JsonValue jsonValue)) {
            context.reportProblem(format("%s VP is not a JSON type: %s", credentialType, vp.getClass().getName()));
            return false;
        }

        if (!(OBJECT == jsonValue.getValueType())) {
            context.reportProblem(format("%s VP must be type %s but was: %s", credentialType, OBJECT, jsonValue.getValueType()));
            return false;
        }

        return true;
    }

    /**
     * Returns the credential subject portion of a VC or null if there was an error. Error information will be reported to the context.
     */
    @Nullable
    protected JsonObject extractCredentialSubject(JsonObject credential, PolicyContext context) {
        var subjectArray = credential.get(CREDENTIAL_SUBJECT);
        if (subjectArray == null || subjectArray.getValueType() != ARRAY) {
            context.reportProblem(errorPrefix + " no credentialSubject found");
            return null;
        }
        if (subjectArray.asJsonArray().size() != 1) {
            context.reportProblem(errorPrefix + " empty credentialSubject");
            return null;
        }

        var subjectValue = subjectArray.asJsonArray().get(0);
        if (subjectValue == null || subjectValue.getValueType() != OBJECT) {
            context.reportProblem(errorPrefix + " invalid credentialSubject format");
            return null;
        }

        return subjectValue.asJsonObject();
    }

    /**
     * Returns true if the actual operand value is a string literal case-insensitive equal to the expected value.
     */
    protected boolean validateRightOperand(String expectedValue, Object actualValue, PolicyContext context) {
        if (!(actualValue instanceof String)) {
            context.reportProblem(format("Invalid right operand format specified for %s credential", credentialType));
            return false;
        }

        if (!expectedValue.equalsIgnoreCase(actualValue.toString().trim())) {
            context.reportProblem(format("Invalid right operand specified for %s credential: %s", credentialType, actualValue));
            return false;
        }

        return true;
    }

}
