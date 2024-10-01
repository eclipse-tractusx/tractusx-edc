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

import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.tractusx.edc.agreements.retirement.function.AgreementsRetirementFunction;
import org.eclipse.tractusx.edc.agreements.retirement.spi.store.AgreementsRetirementStore;

import static org.eclipse.tractusx.edc.agreements.retirement.AgreementsRetirementFunctionExtension.NAME;
import static org.eclipse.edc.connector.controlplane.contract.spi.validation.ContractValidationService.TRANSFER_SCOPE;
import static org.eclipse.edc.connector.policy.monitor.PolicyMonitorExtension.POLICY_MONITOR_SCOPE;


@Extension(value = NAME)
public class AgreementsRetirementFunctionExtension implements ServiceExtension {

    public static final String NAME = "Agreements Retirement Policy Function Extension";

    @Inject
    private AgreementsRetirementStore store;

    @Inject
    private PolicyEngine policyEngine;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var function = new AgreementsRetirementFunction(store);
        policyEngine.registerFunction(TRANSFER_SCOPE, Permission.class, function);
        policyEngine.registerFunction(POLICY_MONITOR_SCOPE, Permission.class, function);
    }
}
