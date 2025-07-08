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

package org.eclipse.tractusx.edc.policy.cx.common;

import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Rule;
import org.eclipse.edc.spi.result.Result;

import java.util.Set;

import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.CX_POLICY_NS;

public abstract class BaseConstraintFunction<T extends Rule, C extends ParticipantAgentPolicyContext> implements AtomicConstraintRuleFunction<T, C> {
    private final Set<Operator> allowedOperators;

    protected BaseConstraintFunction(Set<Operator> allowedOperators) {
        this.allowedOperators = allowedOperators;
    }

    @Override
    public boolean evaluate(Operator operator, Object rightOperand, T rule, C c) {
        return true;
    }

    @Override
    public Result<Void> validate(Operator operator, Object rightValue, T rule) {
        if (!allowedOperators.contains(operator)) {
            return Result.failure("Invalid operator: this constraint only allows the following operators: %s, but received '%s'."
                    .formatted(allowedOperators, operator));
        }
        return validateRightOperand(rightValue);
    }

    protected abstract Result<Void> validateRightOperand(Object rightValue);
}
