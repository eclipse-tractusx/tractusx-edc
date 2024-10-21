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

import org.eclipse.edc.connector.controlplane.contract.spi.policy.TransferProcessPolicyContext;
import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.tractusx.edc.agreements.retirement.spi.service.AgreementsRetirementService;

import static org.eclipse.tractusx.edc.agreements.retirement.AgreementsRetirementPreValidatorRegisterExtension.NAME;


@Extension(value = NAME)
public class AgreementsRetirementPreValidatorRegisterExtension implements ServiceExtension {

    public static final String NAME = "Agreements Retirement Policy Function Extension";

    @Inject
    private AgreementsRetirementService service;

    @Inject
    private PolicyEngine policyEngine;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var validator = new AgreementRetirementValidator(service);
        policyEngine.registerPreValidator(TransferProcessPolicyContext.class, validator.transferProcess());
        policyEngine.registerPreValidator(PolicyMonitorContext.class, validator.policyMonitor());
    }
}
