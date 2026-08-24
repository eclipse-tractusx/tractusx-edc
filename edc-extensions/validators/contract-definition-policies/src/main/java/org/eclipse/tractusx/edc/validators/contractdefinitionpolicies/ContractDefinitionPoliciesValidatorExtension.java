/********************************************************************************
 * Copyright (c) 2026 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.validators.contractdefinitionpolicies;

import org.eclipse.edc.connector.controlplane.services.spi.contractdefinition.ContractDefinitionService;
import org.eclipse.edc.connector.controlplane.services.spi.policydefinition.PolicyDefinitionService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;

import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_ACCESSPOLICY_ID;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_CONTRACTPOLICY_ID;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_TYPE;
import static org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition.EDC_POLICY_DEFINITION_TYPE;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.ACTION_ACCESS;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.ACTION_USAGE;

@Extension(ContractDefinitionPoliciesValidatorExtension.NAME)
public class ContractDefinitionPoliciesValidatorExtension implements ServiceExtension {

    public static final String NAME = "Contract Definition Policies Validator Extension";

    @Inject
    private JsonObjectValidatorRegistry validatorRegistry;

    @Inject
    private PolicyDefinitionService policyDefinitionService;

    @Inject
    private ContractDefinitionService contractDefinitionService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var contractDefinitionsValidator = JsonObjectValidator.newValidator()
                .verify(CONTRACT_DEFINITION_ACCESSPOLICY_ID, path -> new PolicyActionMatchesExpected(path, policyDefinitionService, ACTION_ACCESS))
                .verify(CONTRACT_DEFINITION_CONTRACTPOLICY_ID, path -> new PolicyActionMatchesExpected(path, policyDefinitionService, ACTION_USAGE))
                .build();
        validatorRegistry.register(CONTRACT_DEFINITION_TYPE, contractDefinitionsValidator);

        var policyDefinitionsValidator = JsonObjectValidator.newValidator()
                .verifyId(path -> new NotReferencedByContractDefinition(path, contractDefinitionService))
                .build();
        validatorRegistry.register(EDC_POLICY_DEFINITION_TYPE, policyDefinitionsValidator);
    }
}
