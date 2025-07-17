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

package org.eclipse.tractusx.edc.policy.cx;

import org.eclipse.edc.connector.controlplane.catalog.spi.policy.CatalogPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.ContractNegotiationPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.TransferProcessPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.policy.model.Rule;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.tractusx.edc.policy.cx.affiliates.AffiliatesBpnlPermissionConstraintFunction;
import org.eclipse.tractusx.edc.policy.cx.affiliates.AffiliatesBpnlProhibitionConstraintFunction;
import org.eclipse.tractusx.edc.policy.cx.affiliates.AffiliatesRegionPermissionConstraintFunction;
import org.eclipse.tractusx.edc.policy.cx.affiliates.AffiliatesRegionProhibitionConstraintFunction;
import org.eclipse.tractusx.edc.policy.cx.contractreference.ContractReferenceConstraintFunction;
import org.eclipse.tractusx.edc.policy.cx.dismantler.DismantlerCredentialConstraintFunction;
import org.eclipse.tractusx.edc.policy.cx.framework.FrameworkAgreementCredentialConstraintFunction;
import org.eclipse.tractusx.edc.policy.cx.membership.MembershipCredentialConstraintFunction;
import org.eclipse.tractusx.edc.policy.cx.usage.UsagePurposeConstraintFunction;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_SCHEMA;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.CX_POLICY_NS;
import static org.eclipse.tractusx.edc.policy.cx.affiliates.AffiliatesBpnlProhibitionConstraintFunction.AFFILIATES_BPNL;
import static org.eclipse.tractusx.edc.policy.cx.affiliates.AffiliatesRegionProhibitionConstraintFunction.AFFILIATES_REGION;
import static org.eclipse.tractusx.edc.policy.cx.common.PolicyScopes.CATALOG_REQUEST_SCOPE;
import static org.eclipse.tractusx.edc.policy.cx.common.PolicyScopes.CATALOG_SCOPE;
import static org.eclipse.tractusx.edc.policy.cx.common.PolicyScopes.NEGOTIATION_REQUEST_SCOPE;
import static org.eclipse.tractusx.edc.policy.cx.common.PolicyScopes.NEGOTIATION_SCOPE;
import static org.eclipse.tractusx.edc.policy.cx.common.PolicyScopes.TRANSFER_PROCESS_REQUEST_SCOPE;
import static org.eclipse.tractusx.edc.policy.cx.common.PolicyScopes.TRANSFER_PROCESS_SCOPE;
import static org.eclipse.tractusx.edc.policy.cx.contractreference.ContractReferenceConstraintFunction.CONTRACT_REFERENCE;
import static org.eclipse.tractusx.edc.policy.cx.dismantler.DismantlerCredentialConstraintFunction.DISMANTLER_LITERAL;
import static org.eclipse.tractusx.edc.policy.cx.framework.FrameworkAgreementCredentialConstraintFunction.FRAMEWORK_AGREEMENT_LITERAL;
import static org.eclipse.tractusx.edc.policy.cx.membership.MembershipCredentialConstraintFunction.MEMBERSHIP_LITERAL;
import static org.eclipse.tractusx.edc.policy.cx.usage.UsagePurposeConstraintFunction.USAGE_PURPOSE;


/**
 * Provides implementations of standard CX usage policies.
 */
@Extension(CxPolicyExtension.NAME)
public class CxPolicyExtension implements ServiceExtension {

    public static final String NAME = "CX Policy";
    private static final Set<String> RULE_SCOPES = Set.of(CATALOG_REQUEST_SCOPE, NEGOTIATION_REQUEST_SCOPE,
            TRANSFER_PROCESS_REQUEST_SCOPE, CATALOG_SCOPE, NEGOTIATION_SCOPE, TRANSFER_PROCESS_SCOPE);

    @Inject
    private PolicyEngine policyEngine;

    @Inject
    private RuleBindingRegistry bindingRegistry;

    public static void registerFunctions(PolicyEngine engine) {

        // Usage Prohibition Validators
        registerForContexts(
                ContractNegotiationPolicyContext.class,
                Prohibition.class,
                engine,
                Map.of(
                        CX_POLICY_NS + AFFILIATES_BPNL, new AffiliatesBpnlProhibitionConstraintFunction<>(),
                        CX_POLICY_NS + AFFILIATES_REGION, new AffiliatesRegionProhibitionConstraintFunction<>())
        );

        registerForContexts(
                TransferProcessPolicyContext.class,
                Prohibition.class,
                engine,
                Map.of(
                        CX_POLICY_NS + AFFILIATES_BPNL, new AffiliatesBpnlProhibitionConstraintFunction<>(),
                        CX_POLICY_NS + AFFILIATES_REGION, new AffiliatesRegionProhibitionConstraintFunction<>())
        );

        // Access and Usage Permission Validators
        engine.registerFunction(CatalogPolicyContext.class, Permission.class, new DismantlerCredentialConstraintFunction<>());
        engine.registerFunction(ContractNegotiationPolicyContext.class, Permission.class, new DismantlerCredentialConstraintFunction<>());
        engine.registerFunction(TransferProcessPolicyContext.class, Permission.class, new DismantlerCredentialConstraintFunction<>());

        engine.registerFunction(CatalogPolicyContext.class, Permission.class, new FrameworkAgreementCredentialConstraintFunction<>());
        engine.registerFunction(ContractNegotiationPolicyContext.class, Permission.class, new FrameworkAgreementCredentialConstraintFunction<>());
        engine.registerFunction(TransferProcessPolicyContext.class, Permission.class, new FrameworkAgreementCredentialConstraintFunction<>());

        engine.registerFunction(CatalogPolicyContext.class, Permission.class, new MembershipCredentialConstraintFunction<>());
        engine.registerFunction(ContractNegotiationPolicyContext.class, Permission.class, new MembershipCredentialConstraintFunction<>());
        engine.registerFunction(TransferProcessPolicyContext.class, Permission.class, new MembershipCredentialConstraintFunction<>());

        // Usage Permission Validators
        engine.registerFunction(ContractNegotiationPolicyContext.class, Permission.class, CX_POLICY_NS + AFFILIATES_BPNL, new AffiliatesBpnlPermissionConstraintFunction<>());
        engine.registerFunction(TransferProcessPolicyContext.class, Permission.class, CX_POLICY_NS + AFFILIATES_BPNL, new AffiliatesBpnlPermissionConstraintFunction<>());

        engine.registerFunction(ContractNegotiationPolicyContext.class, Permission.class, CX_POLICY_NS + AFFILIATES_REGION, new AffiliatesRegionPermissionConstraintFunction<>());
        engine.registerFunction(TransferProcessPolicyContext.class, Permission.class, CX_POLICY_NS + AFFILIATES_REGION, new AffiliatesRegionPermissionConstraintFunction<>());

        engine.registerFunction(ContractNegotiationPolicyContext.class, Permission.class, CX_POLICY_NS + CONTRACT_REFERENCE, new ContractReferenceConstraintFunction<>());
        engine.registerFunction(TransferProcessPolicyContext.class, Permission.class, CX_POLICY_NS + CONTRACT_REFERENCE, new ContractReferenceConstraintFunction<>());

        engine.registerFunction(ContractNegotiationPolicyContext.class, Permission.class, CX_POLICY_NS + USAGE_PURPOSE, new UsagePurposeConstraintFunction<>());
        engine.registerFunction(TransferProcessPolicyContext.class, Permission.class, CX_POLICY_NS + USAGE_PURPOSE, new UsagePurposeConstraintFunction<>());
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

        registerBindingSets(
                registry,
                Set.of(CX_POLICY_NS + USAGE_PURPOSE, CX_POLICY_NS + CONTRACT_REFERENCE),
                Set.of(CATALOG_SCOPE, NEGOTIATION_SCOPE, TRANSFER_PROCESS_SCOPE));

        // Usage Prohibition Validators
        registerBindingSets(
                registry,
                Set.of(CX_POLICY_NS + AFFILIATES_BPNL, CX_POLICY_NS + AFFILIATES_REGION),
                Set.of(NEGOTIATION_SCOPE, TRANSFER_PROCESS_SCOPE));

        // Usage Permission Validators
        registerBindingSets(
                registry,
                Set.of(
                        CX_POLICY_NS + AFFILIATES_BPNL,
                        CX_POLICY_NS + AFFILIATES_REGION,
                        CX_POLICY_NS + CONTRACT_REFERENCE,
                        CX_POLICY_NS + USAGE_PURPOSE),
                Set.of(NEGOTIATION_SCOPE, TRANSFER_PROCESS_SCOPE));

    }

    private static void registerBindingSets(RuleBindingRegistry registry, Set<String> names, Set<String> scopes) {
        scopes.forEach(scope -> names.forEach(name -> registry.bind(name, scope)));
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        registerFunctions(policyEngine);
        registerBindings(bindingRegistry);
    }

    private static <R extends Rule, C extends PolicyContext> void registerForContexts(Class<C> context, Class<R> type, PolicyEngine engine, Map<String, AtomicConstraintRuleFunction<R, C>> constraints) {
        constraints.forEach((functionId, constraintFunction) ->
                engine.registerFunction(context, type, functionId, constraintFunction)
        );
    }
}