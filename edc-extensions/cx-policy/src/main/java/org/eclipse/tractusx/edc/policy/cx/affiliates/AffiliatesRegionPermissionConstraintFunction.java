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

package org.eclipse.tractusx.edc.policy.cx.affiliates;

import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.model.Permission;

/**
 * This is a placeholder constraint function for AffiliatesRegion. It always returns true but allows
 * the validation of policies to be strictly enforced.
 */
public class AffiliatesRegionPermissionConstraintFunction<C extends ParticipantAgentPolicyContext> extends AffiliatesRegionBaseConstraintFunction<Permission, C> {
    public AffiliatesRegionPermissionConstraintFunction() {
        super();
    }
}
