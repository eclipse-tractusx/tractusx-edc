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

package org.eclipse.tractusx.edc.agreements.retirement;

import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyValidatorFunction;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.tractusx.edc.agreements.retirement.spi.service.AgreementsRetirementService;


public record AgreementRetirementValidator(AgreementsRetirementService agreementsRetirementService) implements PolicyValidatorFunction {

    @Override
    public Boolean apply(Policy policy, PolicyContext policyContext) {
        var agreement = policyContext.getContextData(ContractAgreement.class);
        if (agreement != null) {
            if (agreementsRetirementService.isRetired(agreement.getId())) {
                policyContext.reportProblem(String.format("Contract Agreement with ID=%s has been retired", agreement.getId()));
                return false;
            }
        }
        return true;
    }
}
