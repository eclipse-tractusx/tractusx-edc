/*
 * Copyright (c) 2026 Materna SE
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

package org.constructx.edc.policy.constructx;

import org.constructx.edc.policy.constructx.membership.MembershipCredentialConstraintFunction;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.Test;

import static org.constructx.edc.policy.constructx.common.PolicyScopes.CATALOG_SCOPE;
import static org.constructx.edc.policy.constructx.common.PolicyScopes.NEGOTIATION_SCOPE;
import static org.constructx.edc.policy.constructx.common.PolicyScopes.TRANSFER_PROCESS_SCOPE;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_SCHEMA;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ConstructxPolicyRegistrationTest {

    @Test
    void registerFunctions_registersMembershipFunction() {
        var engine = mock(PolicyEngine.class);
        var monitor = mock(Monitor.class);

        ConstructxPolicyRegistration.registerFunctions(engine, monitor);

        verify(engine, atLeastOnce()).registerFunction(
                any(),
                eq(Permission.class),
                any(MembershipCredentialConstraintFunction.class)
        );
    }

    @Test
    void registerBindings_bindsOdrlUse() {
        var registry = mock(RuleBindingRegistry.class);

        ConstructxPolicyRegistration.registerBindings(registry);

        verify(registry).bind(ODRL_SCHEMA + "use", CATALOG_SCOPE);
        verify(registry).bind(ODRL_SCHEMA + "use", NEGOTIATION_SCOPE);
        verify(registry).bind(ODRL_SCHEMA + "use", TRANSFER_PROCESS_SCOPE);
    }
}
