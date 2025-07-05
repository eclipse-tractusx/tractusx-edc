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
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.tractusx.edc.policy.cx.contractreference.ContractReferenceConstraintFunction;
import org.eclipse.tractusx.edc.policy.cx.dismantler.DismantlerCredentialConstraintFunction;
import org.eclipse.tractusx.edc.policy.cx.framework.FrameworkAgreementCredentialConstraintFunction;
import org.eclipse.tractusx.edc.policy.cx.membership.MembershipCredentialConstraintFunction;
import org.eclipse.tractusx.edc.policy.cx.usage.UsagePurposeConstraintFunction;
import org.eclipse.tractusx.edc.policy.cx.validator.CxPolicyDefinitionValidator;

import java.util.Set;
import java.util.stream.Stream;

import static org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition.EDC_POLICY_DEFINITION_TYPE;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_SCHEMA;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.CX_POLICY_NS;
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

    @Inject
    JsonObjectValidatorRegistry validatorRegistry;

    public static void registerFunctions(PolicyEngine engine) {
        engine.registerFunction(CatalogPolicyContext.class, Permission.class, new DismantlerCredentialConstraintFunction<>());
        engine.registerFunction(ContractNegotiationPolicyContext.class, Permission.class, new DismantlerCredentialConstraintFunction<>());
        engine.registerFunction(TransferProcessPolicyContext.class, Permission.class, new DismantlerCredentialConstraintFunction<>());

        engine.registerFunction(CatalogPolicyContext.class, Permission.class, new FrameworkAgreementCredentialConstraintFunction<>());
        engine.registerFunction(ContractNegotiationPolicyContext.class, Permission.class, new FrameworkAgreementCredentialConstraintFunction<>());
        engine.registerFunction(TransferProcessPolicyContext.class, Permission.class, new FrameworkAgreementCredentialConstraintFunction<>());

        engine.registerFunction(CatalogPolicyContext.class, Permission.class, new MembershipCredentialConstraintFunction<>());
        engine.registerFunction(ContractNegotiationPolicyContext.class, Permission.class, new MembershipCredentialConstraintFunction<>());
        engine.registerFunction(TransferProcessPolicyContext.class, Permission.class, new MembershipCredentialConstraintFunction<>());

        engine.registerFunction(CatalogPolicyContext.class, Permission.class, CX_POLICY_NS + USAGE_PURPOSE, new UsagePurposeConstraintFunction<>());
        engine.registerFunction(ContractNegotiationPolicyContext.class, Permission.class, CX_POLICY_NS + USAGE_PURPOSE, new UsagePurposeConstraintFunction<>());
        engine.registerFunction(TransferProcessPolicyContext.class, Permission.class, CX_POLICY_NS + USAGE_PURPOSE, new UsagePurposeConstraintFunction<>());

        engine.registerFunction(CatalogPolicyContext.class, Permission.class, CX_POLICY_NS + CONTRACT_REFERENCE, new ContractReferenceConstraintFunction<>());
        engine.registerFunction(ContractNegotiationPolicyContext.class, Permission.class, CX_POLICY_NS + CONTRACT_REFERENCE, new ContractReferenceConstraintFunction<>());
        engine.registerFunction(TransferProcessPolicyContext.class, Permission.class, CX_POLICY_NS + CONTRACT_REFERENCE, new ContractReferenceConstraintFunction<>());
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

        //TODO
        registry.bind(EDC_NAMESPACE + "access", CATALOG_SCOPE);
        registry.bind(EDC_NAMESPACE + "access", NEGOTIATION_SCOPE);
        registry.bind(EDC_NAMESPACE + "access", TRANSFER_PROCESS_SCOPE);

        registry.bind(CX_POLICY_NS + USAGE_PURPOSE, CATALOG_SCOPE);
        registry.bind(CX_POLICY_NS + USAGE_PURPOSE, NEGOTIATION_SCOPE);
        registry.bind(CX_POLICY_NS + USAGE_PURPOSE, TRANSFER_PROCESS_SCOPE);

        registry.bind(CX_POLICY_NS + CONTRACT_REFERENCE, CATALOG_SCOPE);
        registry.bind(CX_POLICY_NS + CONTRACT_REFERENCE, NEGOTIATION_SCOPE);
        registry.bind(CX_POLICY_NS + CONTRACT_REFERENCE, TRANSFER_PROCESS_SCOPE);
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        registerFunctions(policyEngine);
        registerBindings(bindingRegistry);
        validatorRegistry.register(EDC_POLICY_DEFINITION_TYPE, CxPolicyDefinitionValidator.instance());
    }
}
