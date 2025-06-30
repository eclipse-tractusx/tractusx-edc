/*
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
 */

package org.eclipse.tractusx.edc.policy.cx.usage;

import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.result.Result;

import java.util.List;
import java.util.Set;


/**
 * This is a placeholder constraint function for UsagePurpose. It always returns true but allows
 * the validation of policies to be strictly enforced.
 */
public class UsagePurposeConstraintFunction<C extends ParticipantAgentPolicyContext> implements AtomicConstraintRuleFunction<Permission, C> {
    public static final String USAGE_PURPOSE = "UsagePurpose";
    private static final Set<String> POSSIBLE_VALUES = Set.of(
            "cx.core.legalRequirementForThirdparty:1",
            "cx.core.industrycore:1",
            "cx.core.qualityNotifications:1",
            "cx.core.digitalTwinRegistry:1",
            "cx.pcf.base:1",
            "cx.quality.base:1",
            "cx.dcm.base:1",
            "cx.puris.base:1",
            "cx.circular.dpp:1",
            "cx.circular.smc:1",
            "cx.circular.marketplace:1",
            "cx.circular.materialaccounting:1",
            "cx.bpdm.gate.upload:1",
            "cx.bpdm.gate.download:1",
            "cx.bpdm.pool:1",
            "cx.bpdm.vas.countryrisk:1",
            "cx.bpdm.vas.dataquality.upload:1",
            "cx.bpdm.vas.dataquality.download:1",
            "cx.bpdm.vas.bdv.upload:1",
            "cx.bpdm.vas.bdv.download:1",
            "cx.bpdm.vas.fpd.upload:1",
            "cx.bpdm.vas.fpd.download:1",
            "cx.bpdm.vas.swd.upload:1",
            "cx.bpdm.vas.swd.download:1",
            "cx.bpdm.vas.nps.upload:1",
            "cx.bpdm.vas.nps.download:1",
            "cx.ccm.base:1",
            "cx.bpdm.poolAll:1",
            "cx.logistics.base:1"
    );
    private static final Set<Operator> ALLOWED_OPERATORS = Set.of(
            Operator.IS_ALL_OF
    );

    @Override
    public boolean evaluate(Operator operator, Object rightOperand, Permission permission, C c) {
        return true;
    }

    @Override
    public Result<Void> validate(Operator operator, Object rightValue, Permission rule) {
        if (!ALLOWED_OPERATORS.contains(operator)) {
            return Result.failure("Invalid operator: this constraint only allows the following operators: %s, but received '%s'.".formatted(ALLOWED_OPERATORS, operator));
        }

        if (!(rightValue instanceof List<?> list) || list.isEmpty()) {
            return Result.failure("Invalid right-operand: must be a list and contain at least 1 value");
        }

        var invalidValues = list.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(value -> !POSSIBLE_VALUES.contains(value))
                .toList();

        return invalidValues.isEmpty() ?
                Result.success() :
                Result.failure("Invalid right-operand: the following values are not allowed: %s".formatted(invalidValues));
    }
}
