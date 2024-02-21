/*
 * Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.policy.cx;

import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.tractusx.edc.policy.cx.dismantler.DismantlerCredentialConstraintFunction;
import org.eclipse.tractusx.edc.policy.cx.framework.FrameworkAgreementCredentialConstraintFunction;
import org.eclipse.tractusx.edc.policy.cx.membership.MembershipCredentialConstraintFunction;

import java.util.Set;
import java.util.stream.Stream;

import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_SCHEMA;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.CX_POLICY_NS;
import static org.eclipse.tractusx.edc.policy.cx.common.PolicyScopes.CATALOG_REQUEST_SCOPE;
import static org.eclipse.tractusx.edc.policy.cx.common.PolicyScopes.CATALOG_SCOPE;
import static org.eclipse.tractusx.edc.policy.cx.common.PolicyScopes.NEGOTIATION_REQUEST_SCOPE;
import static org.eclipse.tractusx.edc.policy.cx.common.PolicyScopes.NEGOTIATION_SCOPE;
import static org.eclipse.tractusx.edc.policy.cx.common.PolicyScopes.TRANSFER_PROCESS_REQUEST_SCOPE;
import static org.eclipse.tractusx.edc.policy.cx.common.PolicyScopes.TRANSFER_PROCESS_SCOPE;
import static org.eclipse.tractusx.edc.policy.cx.dismantler.DismantlerCredentialConstraintFunction.DISMANTLER_LITERAL;
import static org.eclipse.tractusx.edc.policy.cx.framework.FrameworkAgreementCredentialConstraintFunction.FRAMEWORK_AGREEMENT_LITERAL;
import static org.eclipse.tractusx.edc.policy.cx.membership.MembershipCredentialConstraintFunction.MEMBERSHIP_LITERAL;

public class CxPolicyRegistration {
    private static final Set<String> FUNCTION_SCOPES = Set.of(CATALOG_SCOPE, NEGOTIATION_SCOPE, TRANSFER_PROCESS_SCOPE);
    private static final Set<String> RULE_SCOPES = Set.of(CATALOG_REQUEST_SCOPE, NEGOTIATION_REQUEST_SCOPE, TRANSFER_PROCESS_REQUEST_SCOPE, CATALOG_SCOPE, NEGOTIATION_SCOPE, TRANSFER_PROCESS_SCOPE);

    public static void registerFunctions(PolicyEngine engine) {
        FUNCTION_SCOPES.forEach(scope -> {
            engine.registerFunction(scope, Permission.class, new DismantlerCredentialConstraintFunction());
            engine.registerFunction(scope, Permission.class, new MembershipCredentialConstraintFunction());
            engine.registerFunction(scope, Permission.class, new FrameworkAgreementCredentialConstraintFunction());
        });
    }

    public static void registerBindings(RuleBindingRegistry registry) {
        registry.dynamicBind(s -> {
            if (Stream.of(FRAMEWORK_AGREEMENT_LITERAL, DISMANTLER_LITERAL, MEMBERSHIP_LITERAL).anyMatch(postfix -> s.startsWith(CX_POLICY_NS + postfix))) {
                return RULE_SCOPES;
            }
            return Set.of();
        });

        registry.bind(ODRL_SCHEMA + "use", CATALOG_SCOPE);
        registry.bind(ODRL_SCHEMA + "use", NEGOTIATION_SCOPE);
        registry.bind(ODRL_SCHEMA + "use", TRANSFER_PROCESS_SCOPE);
    }
}
