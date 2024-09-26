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

package org.eclipse.tractusx.edc.agreements.retirement.function;

import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.engine.spi.RuleFunction;
import org.eclipse.edc.policy.model.Rule;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.tractusx.edc.agreements.retirement.spi.AgreementsRetirementStore;


public class AgreementsRetirementFunction implements RuleFunction {

    Monitor monitor;
    AgreementsRetirementStore store;

    AgreementsRetirementFunction(Monitor monitor, AgreementsRetirementStore store) {
        this.monitor = monitor;
        this.store = store;
    }

    @Override
    public boolean evaluate(Rule rule, PolicyContext policyContext) {
        var agreement = policyContext.getContextData(ContractAgreement.class);
        var isActive = true;
        if (agreement == null) {
            policyContext.reportProblem("Tried to evaluate agreement retirement function but policyContext has no agreement defined.");
            return isActive;
        }
        var result = store.findRetiredAgreement(agreement.getId());
        if (result.succeeded()) {
            isActive = false;
        }

        return isActive;
    }
}
