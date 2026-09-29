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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PERMISSION_ATTRIBUTE;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.CX_POLICY_NS;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyBuilderFixtures.atomicConstraint;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyBuilderFixtures.policy;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyBuilderFixtures.rule;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.ACTION_USAGE;
import static org.eclipse.tractusx.edc.policy.cx.validator.PolicyValidationConstants.FRAMEWORK_AGREEMENT_LITERAL;

class LegacyPolicyCheckTest {
    
    @Test
    void legacyConstraint_shouldReturnTrue() {
        var permission = rule(ACTION_USAGE, atomicConstraint(CX_POLICY_NS + "FrameworkAgreement"));
        var policy = policy(ODRL_PERMISSION_ATTRIBUTE, permission);
        
        var result = LegacyPolicyCheck.isLegacy(policy);
        
        assertThat(result).isTrue();
    }
    
    @Test
    void noLegacyConstraint_shouldReturnFalse() {
        var permission = rule(ACTION_USAGE, atomicConstraint(FRAMEWORK_AGREEMENT_LITERAL));
        var policy = policy(ODRL_PERMISSION_ATTRIBUTE, permission);
        
        var result = LegacyPolicyCheck.isLegacy(policy);
        
        assertThat(result).isFalse();
    }
    
    @Test
    void legacyBpnConstraint_shouldReturnTrue() {
        var permission = rule(ACTION_USAGE, atomicConstraint("https://w3id.org/tractusx/v0.0.1/ns/BusinessPartnerNumber"));
        var policy = policy(ODRL_PERMISSION_ATTRIBUTE, permission);
        
        var result = LegacyPolicyCheck.isLegacy(policy);
        
        assertThat(result).isTrue();
    }
    
    @Test
    void legacyBpnGroupConstraint_shouldReturnTrue() {
        var permission = rule(ACTION_USAGE, atomicConstraint("https://w3id.org/tractusx/v0.0.1/ns/BusinessPartnerGroup"));
        var policy = policy(ODRL_PERMISSION_ATTRIBUTE, permission);
        
        var result = LegacyPolicyCheck.isLegacy(policy);
        
        assertThat(result).isTrue();
    }

    @Test
    void legacyInForceDateConstraint_shouldReturnTrue() {
        var permission = rule(ACTION_USAGE, atomicConstraint("https://w3id.org/edc/v0.0.1/ns/inForceDate"));
        var policy = policy(ODRL_PERMISSION_ATTRIBUTE, permission);

        var result = LegacyPolicyCheck.isLegacy(policy);

        assertThat(result).isTrue();
    }
}
