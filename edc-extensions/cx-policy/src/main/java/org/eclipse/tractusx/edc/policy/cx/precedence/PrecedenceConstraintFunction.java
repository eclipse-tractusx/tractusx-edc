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

package org.eclipse.tractusx.edc.policy.cx.precedence;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.tractusx.edc.policy.cx.common.AbstractDynamicCredentialConstraintFunction;

import java.util.Set;

import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.CX_POLICY_NS;


/**
 * This constraint function checks that a MembershipCredential is present in a list of {@link VerifiableCredential}
 * objects extracted from a {@link ParticipantAgent} which is expected to be present on the {@link PolicyContext}.
 */
public class PrecedenceConstraintFunction<C extends ParticipantAgentPolicyContext> extends AbstractDynamicCredentialConstraintFunction<C> {
    public static final String PRECEDENCE_LITERAL = "Precedence";
    public static final Set<String> VALID_VALUES = Set.of(
            "cx.precedence.contractReference:1",
            "cx.precedence.rcAgreement:1"
    );
    private static final Set<Operator> ALLOWED_OPERATORS = Set.of(
            Operator.EQ
    );

    @Override
    public boolean evaluate(Object leftOperand, Operator operator, Object rightOperand, Permission permission, C context) {
        if (!VALID_VALUES.contains(rightOperand.toString().toLowerCase())) {
            context.reportProblem("Right-operand must be equal to '%s', but was '%s'".formatted(String.join(", ", VALID_VALUES), rightOperand));
            return false;
        }
        if (!(CX_POLICY_NS + PRECEDENCE_LITERAL).equalsIgnoreCase(leftOperand.toString())) {
            context.reportProblem("Invalid left-operand: must be '%s', but was '%s'".formatted(PRECEDENCE_LITERAL, leftOperand));
            return false;
        }

        return true;
    }

    @Override
    public boolean canHandle(Object leftOperand) {
        return leftOperand instanceof String && (CX_POLICY_NS + PRECEDENCE_LITERAL).equalsIgnoreCase(leftOperand.toString());
    }

    @Override
    public Result<Void> validate(Object leftValue, Operator operator, Object rightValue, Permission rule) {
        if (!ALLOWED_OPERATORS.contains(operator)) {
            return Result.failure("Invalid operator: this constraint only allows the following operators: %s, but received '%s'.".formatted(ALLOWED_OPERATORS, operator));
        }
        return rightValue instanceof String && VALID_VALUES.contains(rightValue.toString().toLowerCase())
                ? Result.success()
                : Result.failure("Invalid right-operand: this constraint only allows the following right-operands: %s, but received '%s'."
                .formatted(String.join(", ", VALID_VALUES), rightValue));
    }
}