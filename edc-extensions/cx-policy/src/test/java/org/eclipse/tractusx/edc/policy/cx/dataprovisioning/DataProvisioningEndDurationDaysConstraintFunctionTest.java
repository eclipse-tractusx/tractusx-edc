/********************************************************************************
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
 ********************************************************************************/

package org.eclipse.tractusx.edc.policy.cx.dataprovisioning;

import org.eclipse.edc.connector.controlplane.contract.spi.policy.AgreementPolicyContext;
import org.eclipse.edc.policy.model.Duty;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;

import static org.assertj.core.api.Assertions.assertThat;

class DataProvisioningEndDurationDaysConstraintFunctionTest {

    private final DataProvisioningEndDurationDaysConstraintFunction<AgreementPolicyContext> function = new DataProvisioningEndDurationDaysConstraintFunction<>();

    @Test
    void shouldOnlyApplyToDuty() {
        // Ensure that the function is parameterized with the Duty class, which means it will only apply to Duty rules
        var superclass = (ParameterizedType) function.getClass().getGenericSuperclass();
        var ruleType = superclass.getActualTypeArguments()[0];
        assertThat(ruleType).isEqualTo(Duty.class);
    }
}
