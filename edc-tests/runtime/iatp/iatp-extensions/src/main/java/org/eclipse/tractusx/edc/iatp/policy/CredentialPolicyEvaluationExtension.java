/********************************************************************************
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
 ********************************************************************************/

package org.eclipse.tractusx.edc.iatp.policy;

import org.eclipse.edc.policy.engine.spi.AtomicConstraintFunction;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.agent.ParticipantAgentService;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_SCHEMA;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.TX_CREDENTIAL_NAMESPACE;

@Extension("TX credential policy evaluation extension")
public class CredentialPolicyEvaluationExtension implements ServiceExtension {

    public static final String MEMBERSHIP_CONSTRAINT_KEY = "Membership";
    private static final String CATALOG_SCOPE = "catalog";
    private static final String NEGOTIATION_SCOPE = "contract.negotiation";
    private static final String TRANSFER_PROCESS_SCOPE = "transfer.process";

    @Inject
    private PolicyEngine policyEngine;

    @Inject
    private RuleBindingRegistry ruleBindingRegistry;


    @Inject
    private ParticipantAgentService participantAgentService;

    @Override
    public void initialize(ServiceExtensionContext context) {

        var fct = new MembershipCredentialEvaluationFunction();

        bindPermissionFunction(fct, TRANSFER_PROCESS_SCOPE, MEMBERSHIP_CONSTRAINT_KEY);
        bindPermissionFunction(fct, NEGOTIATION_SCOPE, MEMBERSHIP_CONSTRAINT_KEY);
        bindPermissionFunction(fct, CATALOG_SCOPE, MEMBERSHIP_CONSTRAINT_KEY);

        registerUseCase("pcf");
        registerUseCase("traceability");
        registerUseCase("sustainability");
        registerUseCase("quality");
        registerUseCase("resiliency");

        participantAgentService.register(new IdentityExtractor());
    }

    private void bindPermissionFunction(AtomicConstraintFunction<Permission> function, String scope, String constraintType) {
        ruleBindingRegistry.bind("USE", scope);
        ruleBindingRegistry.bind(ODRL_SCHEMA + "use", scope);
        ruleBindingRegistry.bind(constraintType, scope);
        ruleBindingRegistry.bind(TX_CREDENTIAL_NAMESPACE + constraintType, scope);

        policyEngine.registerFunction(scope, Permission.class, constraintType, function);
        policyEngine.registerFunction(scope, Permission.class, TX_CREDENTIAL_NAMESPACE + constraintType, function);
    }

    private void registerUseCase(String useCaseName) {
        var frameworkFunction = new FrameworkCredentialEvaluationFunction(useCaseName);
        var usecase = frameworkFunction.key();

        bindPermissionFunction(frameworkFunction, TRANSFER_PROCESS_SCOPE, usecase);
        bindPermissionFunction(frameworkFunction, NEGOTIATION_SCOPE, usecase);
        bindPermissionFunction(frameworkFunction, CATALOG_SCOPE, usecase);
    }
}
