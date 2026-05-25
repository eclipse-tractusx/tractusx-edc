/*
 * Copyright (c) 2024 T-Systems International GmbH
 * Copyright (c) 2025 SAP SE
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
 */

package org.constructx.edc.policy.constructx;

import org.constructx.edc.policy.constructx.membership.MembershipCredentialConstraintFunction;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.Set;
import java.util.stream.Stream;

import static org.constructx.edc.policy.constructx.ConstructxPolicyConstants.CONSTRUCTX_POLICY_NS;
import static org.constructx.edc.policy.constructx.common.PolicyScopes.CATALOG_REQUEST_SCOPE;
import static org.constructx.edc.policy.constructx.common.PolicyScopes.CATALOG_SCOPE;
import static org.constructx.edc.policy.constructx.common.PolicyScopes.CATALOG_SCOPE_CLASS;
import static org.constructx.edc.policy.constructx.common.PolicyScopes.NEGOTIATION_REQUEST_SCOPE;
import static org.constructx.edc.policy.constructx.common.PolicyScopes.NEGOTIATION_SCOPE;
import static org.constructx.edc.policy.constructx.common.PolicyScopes.NEGOTIATION_SCOPE_CLASS;
import static org.constructx.edc.policy.constructx.common.PolicyScopes.TRANSFER_PROCESS_REQUEST_SCOPE;
import static org.constructx.edc.policy.constructx.common.PolicyScopes.TRANSFER_PROCESS_SCOPE;
import static org.constructx.edc.policy.constructx.common.PolicyScopes.TRANSFER_PROCESS_SCOPE_CLASS;
import static org.constructx.edc.policy.constructx.membership.MembershipCredentialConstraintFunction.CONSTRUCTX_MEMBERSHIP_LITERAL;
import static org.constructx.edc.policy.constructx.membership.MembershipCredentialConstraintFunction.MEMBERSHIP_LITERAL;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_SCHEMA;

/**
 * Registers Construct-X policy constraints to the EDC.
 */
public class ConstructxPolicyRegistration {
    private static final Set<Class> FUNCTION_SCOPES_CLASSES =
            Set.of(CATALOG_SCOPE_CLASS, NEGOTIATION_SCOPE_CLASS, TRANSFER_PROCESS_SCOPE_CLASS);

    private static final Set<String> RULE_SCOPES =
            Set.of(CATALOG_REQUEST_SCOPE, NEGOTIATION_REQUEST_SCOPE, TRANSFER_PROCESS_REQUEST_SCOPE,
                    CATALOG_SCOPE, NEGOTIATION_SCOPE, TRANSFER_PROCESS_SCOPE);

    public static void registerFunctions(PolicyEngine engine, Monitor monitor) {
        FUNCTION_SCOPES_CLASSES.forEach(scope ->
                engine.registerFunction(scope, Permission.class, new MembershipCredentialConstraintFunction<>(monitor))
        );
    }

    public static void registerBindings(RuleBindingRegistry registry) {
        registry.dynamicBind(s -> {
            if (Stream.of(MEMBERSHIP_LITERAL, CONSTRUCTX_MEMBERSHIP_LITERAL)
                    .anyMatch(postfix -> s.startsWith(CONSTRUCTX_POLICY_NS + postfix))) {
                return RULE_SCOPES;
            }
            return Set.of();
        });

        registry.bind(ODRL_SCHEMA + "use", CATALOG_SCOPE);
        registry.bind(ODRL_SCHEMA + "use", NEGOTIATION_SCOPE);
        registry.bind(ODRL_SCHEMA + "use", TRANSFER_PROCESS_SCOPE);
    }
}