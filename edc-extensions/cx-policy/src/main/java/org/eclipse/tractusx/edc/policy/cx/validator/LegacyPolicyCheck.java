/********************************************************************************
 * Copyright (c) 2025 Cofinity-X GmbH
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

package org.eclipse.tractusx.edc.policy.cx.validator;

import jakarta.json.JsonObject;

import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.CX_POLICY_NS;

/**
 * Provides a utility method for determining whether a given policy is a legacy policy.
 */
public class LegacyPolicyCheck {
    
    private static final String BPN_LEGACY_LEFT_OPERAND = "https://w3id.org/tractusx/v0.0.1/ns/BusinessPartnerNumber";
    private static final String BPN_GROUP_LEGACY_LEFT_OPERAND = "https://w3id.org/tractusx/v0.0.1/ns/BusinessPartnerGroup";
    
    private LegacyPolicyCheck() {}
    
    /**
     * Checks whether a policy is a legacy policy. Returns true, if the legacy namespace is found
     * or a legacy BPN constraint, which utilizes a different namespace.
     *
     * @param policy the policy
     * @return true, if any legacy reference is encountered; false otherwise
     */
    public static boolean isLegacy(JsonObject policy) {
        var json = policy.toString();
        
        if (json.contains(CX_POLICY_NS)) {
            return true;
        } else {
            return json.contains(BPN_LEGACY_LEFT_OPERAND) || json.contains(BPN_GROUP_LEGACY_LEFT_OPERAND);
        }
    }
    
}
