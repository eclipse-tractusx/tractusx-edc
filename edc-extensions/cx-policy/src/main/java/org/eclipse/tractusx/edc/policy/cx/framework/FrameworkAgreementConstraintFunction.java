/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.policy.cx.framework;

import jakarta.json.JsonObject;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.eclipse.tractusx.edc.policy.cx.common.AbstractVpConstraintFunction;

import java.util.Objects;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.eclipse.edc.policy.model.Operator.EQ;
import static org.eclipse.edc.policy.model.Operator.GEQ;
import static org.eclipse.edc.policy.model.Operator.GT;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.CredentialsNamespaces.CX_USE_CASE_NS;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.CredentialsNamespaces.VP_PROPERTY;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.JsonLdTypeFunctions.extractObjectsOfType;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.JsonLdValueFunctions.extractStringValue;


/**
 * Enforces a Framework Agreement constraint.
 * <p>
 * A policy constraints requiring a usecase framework credential take a left operand in the form:
 * <pre>FrameworkAgreement.[type]</pre>
 * <p>
 * The following example requires a client to present a sustainability credential:
 * <pre>
 * "constraint": {
 *     "leftOperand": "FrameworkAgreement.sustainability",
 *     "operator": "eq",
 *     "rightOperand": "active"
 * }
 * </pre>
 * <p>
 * NB: This function will be enabled in the 3.2 release.
 */
public class FrameworkAgreementConstraintFunction extends AbstractVpConstraintFunction {
    public static final String CONTRACT_VERSION_PROPERTY = CX_USE_CASE_NS + "/contractVersion";
    private static final String ACTIVE = "active";
    private String agreementType;
    private String agreementVersion;

    private FrameworkAgreementConstraintFunction(String credentialType) {
        super(credentialType);
    }

    @Override
    public boolean evaluate(Operator operator, Object rightValue, Permission permission, PolicyContext context) {
        if (!validateOperator(operator, context, EQ, GT, GEQ)) {
            return false;
        }

        if (!validateRightOperand(ACTIVE, rightValue, context)) {
            return false;
        }

        var vp = (JsonObject) context.getContextData(ParticipantAgent.class).getClaims().get(VP_PROPERTY);
        if (!validatePresentation(vp, context)) {
            return false;
        }

        return extractObjectsOfType(credentialType, vp)
                .map(credential -> extractCredentialSubject(credential, context))
                .filter(Objects::nonNull)
                .anyMatch(credentialSubject -> validateUseCase(credentialSubject, operator, context));
    }

    private boolean validateUseCase(JsonObject credentialSubject, Operator operator, PolicyContext context) {
        var usecaseAgreement = extractObjectsOfType(agreementType, credentialSubject).findFirst().orElse(null);
        if (usecaseAgreement == null) {
            context.reportProblem(format("%s is missing the usecase type: %s", credentialType, agreementType));
            return false;
        }

        return validateVersion(context, operator, usecaseAgreement);
    }

    private boolean validateVersion(PolicyContext context, Operator operator, JsonObject usecaseAgreement) {
        if (agreementVersion == null) {
            return true;
        }
        var version = extractStringValue(usecaseAgreement.get(CONTRACT_VERSION_PROPERTY));
        if (version == null || version.trim().length() == 0) {
            context.reportProblem(format("%s is missing a %s property", credentialType, CONTRACT_VERSION_PROPERTY));
            return false;
        }

        switch (operator) {
            case EQ -> {
                if (!version.equals(agreementVersion)) {
                    context.reportProblem(format("%s version %s does not match required version: %s", credentialType, version, agreementVersion));
                    return false;
                }
                return true;
            }
            case GT -> {
                if (version.compareTo(agreementVersion) <= 0) {
                    context.reportProblem(format("%s version %s must be at greater than the required version: %s", credentialType, version, agreementVersion));
                    return false;
                }
                return true;
            }
            case GEQ -> {
                if (version.compareTo(agreementVersion) < 0) {
                    context.reportProblem(format("%s version %s must be at least the required version: %s", credentialType, version, agreementVersion));
                    return false;
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    /**
     * Configures a new constraint instance.
     */
    public static class Builder {
        private final FrameworkAgreementConstraintFunction constraint;

        private Builder(String credentialType) {
            constraint = new FrameworkAgreementConstraintFunction(credentialType);
        }

        /**
         * Sets the framework agreement type.
         */
        public Builder agreementType(String agreementType) {
            constraint.agreementType = agreementType;
            return this;
        }

        /**
         * Sets the optional required agreement version. Equals, greater than, and greater than or equals operations are supported.
         */
        public Builder agreementVersion(String version) {
            constraint.agreementVersion = version;
            return this;
        }

        public FrameworkAgreementConstraintFunction build() {
            requireNonNull(constraint.agreementType, "agreementType");
            return constraint;
        }

        /**
         * Ctor.
         *
         * @param credentialType the framework credential type required by the constraint instance.
         * @return the builder
         */
        public static Builder newInstance(String credentialType) {
            return new Builder(credentialType);
        }
    }


}
