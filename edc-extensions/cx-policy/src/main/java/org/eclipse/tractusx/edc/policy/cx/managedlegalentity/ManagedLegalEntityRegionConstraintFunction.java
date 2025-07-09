/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.edc.policy.cx.managedlegalentity;

import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.tractusx.edc.policy.cx.common.ValueValidatingConstraintFunction;

import java.util.Set;

/**
 * This is a placeholder constraint function for ManagedLegalEntityRegion. It always returns true but allows
 * the validation of policies to be strictly enforced.
 */
public class ManagedLegalEntityRegionConstraintFunction<C extends ParticipantAgentPolicyContext> extends ValueValidatingConstraintFunction<Permission, C> {
    public static final String MANAGED_LEGAL_ENTITY_REGION = "ManagedLegalEntityRegion";

    public ManagedLegalEntityRegionConstraintFunction() {
        super(
                Set.of(Operator.IS_ANY_OF),
                Set.of(
                        "cx.region.all:1",
                        "cx.region.europe:1",
                        "cx.region.northAmerica:1",
                        "cx.region.southAmerica:1",
                        "cx.region.africa:1",
                        "cx.region.asia:1",
                        "cx.region.oceania:1",
                        "cx.region.antarctica:1"
                ),
                true
        );
    }
}
