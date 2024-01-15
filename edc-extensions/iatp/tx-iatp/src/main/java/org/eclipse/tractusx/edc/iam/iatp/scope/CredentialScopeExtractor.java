/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.iam.iatp.scope;

import org.eclipse.edc.identitytrust.scope.ScopeExtractor;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Operator;

import java.util.Set;

import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_CREDENTIAL_NAMESPACE;
import static org.eclipse.tractusx.edc.iam.iatp.TxIatpConstants.CREDENTIAL_TYPE_NAMESPACE;

/**
 * Extract credentials from the policy constraints
 * <p>
 * Extract and map from the Credential DSL the required credential type
 * The left operand should be bound to the namespace {@link org.eclipse.tractusx.edc.edr.spi.CoreConstants#TX_CREDENTIAL_NAMESPACE}
 */
public class CredentialScopeExtractor implements ScopeExtractor {
    public static final String FRAMEWORK_CREDENTIAL_PREFIX = "FrameworkAgreement.";

    public CredentialScopeExtractor() {
    }

    @Override
    public Set<String> extractScopes(Object leftValue, Operator operator, Object rightValue, PolicyContext context) {
        Set<String> scopes = Set.of();
        if (leftValue instanceof String leftOperand && leftOperand.startsWith(TX_CREDENTIAL_NAMESPACE)) {
            var credential = leftOperand.replace(TX_CREDENTIAL_NAMESPACE, "");
            if (credential.startsWith(FRAMEWORK_CREDENTIAL_PREFIX)) {
                credential = credential.replace(FRAMEWORK_CREDENTIAL_PREFIX, "");
            }
            scopes = Set.of("%s:%s:read".formatted(CREDENTIAL_TYPE_NAMESPACE, "%sCredential".formatted(capitalize(credential))));
            
        }
        return scopes;
    }

    private String capitalize(String input) {
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }
}
