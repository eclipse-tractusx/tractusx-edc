/********************************************************************************
 * Copyright (c) 2024 T-Systems International GmbH
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

package org.constructx.edc.policy.constructx;

import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import static org.constructx.edc.policy.constructx.ConstructxPolicyRegistration.registerBindings;
import static org.constructx.edc.policy.constructx.ConstructxPolicyRegistration.registerFunctions;


/**
 * Provides implementations of standard Construct-X usage policies.
 */
public class ConstructxPolicyExtension implements ServiceExtension {
    private static final String NAME = "Construct-X Policy";

    @Inject
    private PolicyEngine policyEngine;

    @Inject
    private Monitor monitor;

    @Inject
    private RuleBindingRegistry bindingRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        registerFunctions(policyEngine, monitor);
        registerBindings(bindingRegistry);
    }
}
