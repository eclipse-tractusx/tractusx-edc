/********************************************************************************
 * Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.tractusx.edc.policy.cx.framework;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.tractusx.edc.core.utils.credentials.CredentialTypePredicate;
import org.eclipse.tractusx.edc.policy.cx.common.AbstractDynamicCredentialConstraintFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.CX_CREDENTIAL_NS;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.CX_POLICY_2025_09_NS;


/**
 * Enforces a Framework Agreement constraint.
 * <p>
 * This function can parse "FrameworkAgreement" constraints as defined in this <a href="https://github.com/eclipse-tractusx/tractusx-profiles/blob/main/cx/policy/specs/policy.definitions.md#4-framework-agreement-constraints">documentation</a>:
 * <pre>
 *     FrameworkAgreement EQ subtype[:version]
 * </pre>
 * In addition, it will support the "legacy" notation, where the subtype is encoded in the left-operand:
 * <pre>
 *     FrameworkAgreement.subtype EQ active[:version]
 * </pre>
 * Either notation is converted into a set of predicates which are applied to the list of credentials. If the resulting filtered list is empty, the
 * policy is considered <strong>not fulfilled</strong>. Note that if the {@code version} is specified, it <strong>must</strong> be satisfied by the <strong>same</strong>
 * credential that satisfies the {@code subtype} requirement.
 */
public class FrameworkAgreementConstraintFunction<C extends ParticipantAgentPolicyContext> extends AbstractDynamicCredentialConstraintFunction<C> {
    public static final String CONTRACT_VERSION_LITERAL = "contractVersion";
    public static final String FRAMEWORK_AGREEMENT_LITERAL = "FrameworkAgreement";

    /**
     * Evaluates the constraint's left-operand and right-operand against a list of {@link VerifiableCredential} objects.
     *
     * @param leftValue  the left-side expression for the constraint. Must be either {@code FrameworkAgreement} or {@code FrameworkAgreement.subtype}.
     * @param operator   the operation Must be {@link Operator#EQ} or {@link Operator#NEQ}
     * @param rightValue the right-side expression for the constraint. Must be a string that is either {@code "active":[version]} or {@code subtype[:version]}.
     * @param permission the permission associated with the constraint. Ignored by this function.
     * @param context    the policy context. Must contain the {@link org.eclipse.edc.participant.spi.ParticipantAgent}, which in turn must contain a list of {@link VerifiableCredential} stored
     *                   in its claims using the {@code "vc"} key.
     * @return true if at least one credential satisfied the requirement imposed by the constraint.
     */
    @Override
    public boolean evaluate(Object leftValue, Operator operator, Object rightValue, Permission permission, C context) {
        var participantAgent = context.participantAgent();

        if (!checkOperator(operator, context, EQUALITY_OPERATORS)) {
            return false;
        }

        // we do not support list-type right-operands
        if (!(leftValue instanceof String) || !(rightValue instanceof String)) {
            context.reportProblem("Both the right- and left-operand must be of type String but were '%s' and '%s', respectively.".formatted(leftValue.getClass(), rightValue.getClass()));
            return false;
        }

        var leftOperand = leftValue.toString();
        var rightOperand = rightValue.toString();
        Result<List<Predicate<VerifiableCredential>>> predicateResult;

        if (leftOperand.startsWith(CX_POLICY_2025_09_NS + FRAMEWORK_AGREEMENT_LITERAL + ".")) { // legacy notation
            predicateResult = getFilterPredicateLegacy(leftOperand, rightOperand);

        } else if (leftOperand.startsWith(CX_POLICY_2025_09_NS + FRAMEWORK_AGREEMENT_LITERAL)) { // new notation
            predicateResult = getFilterPredicate(rightOperand);
        } else { //invalid notation
            context.reportProblem("Constraint left-operand must start with '%s' but was '%s'.".formatted(FRAMEWORK_AGREEMENT_LITERAL, leftValue));
            return false;
        }

        if (predicateResult.failed()) { // couldn't extract subtype/version predicate from constraint
            context.reportProblem(predicateResult.getFailureDetail());
            return false;
        }

        var vcListResult = getCredentialList(participantAgent);
        if (vcListResult.failed()) { // couldn't extract credential list from agent
            context.reportProblem(vcListResult.getFailureDetail());
            return false;
        }
        var rootPredicate = reducePredicates(predicateResult.getContent(), operator);
        var credentials = vcListResult.getContent().stream().filter(rootPredicate).toList();

        if (credentials.isEmpty()) {
            context.reportProblem("No credentials found that match the give Policy constraint: [%s %s %s]".formatted(leftValue.toString(), operator.toString(), rightValue.toString()));
            return false;
        }
        return true;
    }

    /**
     * Returns {@code true} if the left-operand starts with {@link FrameworkAgreementConstraintFunction#FRAMEWORK_AGREEMENT_LITERAL}, {@code false} otherwise.
     */
    @Override
    public boolean canHandle(Object leftValue) {
        return leftValue instanceof String && leftValue.toString().startsWith(CX_POLICY_2025_09_NS + FRAMEWORK_AGREEMENT_LITERAL);
    }

    @Override
    public Result<Void> validate(Object leftOperand, Operator operator, Object rightValue, Permission rule) {
        if (!Operator.EQ.equals(operator)) {
            return Result.failure("Invalid operator: this constraint only allows the following operators: %s, but received '%s'.".formatted(Operator.EQ, operator));
        }

        return rightValue instanceof String s && s.equals("DataExchangeGovernance:1.0") ?
                Result.success() :
                Result.failure("Invalid right-operand: allowed values are '%s'."
                        .formatted("DataExchangeGovernance:1.0"));
    }

    @NotNull
    private Predicate<VerifiableCredential> reducePredicates(List<Predicate<VerifiableCredential>> predicates, Operator operator) {
        return Operator.EQ.equals(operator) ?
                predicates.stream().reduce(Predicate::and).orElse(x -> true) :
                predicates.stream().map(Predicate::negate).reduce(Predicate::and).orElse(x -> true);
    }

    /**
     * Converts the right-operand (new notation) into either 1 or 2 predicates, depending on whether the version was encoded or not.
     */
    private Result<List<Predicate<VerifiableCredential>>> getFilterPredicate(String rightOperand) {
        var tokens = rightOperand.split(":");
        if (tokens.length > 2 || tokens.length == 0 || tokens[0] == null || tokens[0].isEmpty()) {
            return Result.failure("Right-operand must contain the sub-type followed by an optional version string: <subtype>[:version], but was '%s'.".formatted(rightOperand));
        }
        var subtype = tokens[0];
        var version = tokens.length == 2 ? tokens[1] : null;

        return Result.success(createPredicates(subtype, version));
    }

    /**
     * Converts the left- and right-operand (legacy notation) into either 1 or 2 predicates, depending on whether the version was encoded or not.
     */
    private Result<List<Predicate<VerifiableCredential>>> getFilterPredicateLegacy(String leftOperand, String rightOperand) {
        var subType = leftOperand.replace(CX_POLICY_2025_09_NS + FRAMEWORK_AGREEMENT_LITERAL + ".", "");
        if (subType.isEmpty()) {
            return Result.failure("Left-operand must contain the sub-type 'FrameworkAgreement.<subtype>'.");
        }
        if (!rightOperand.startsWith(ACTIVE)) {
            return Result.failure("Right-operand must contain the keyword 'active' followed by an optional version string: 'active'[:version], but was '%s'.".formatted(rightOperand));
        }
        var version = rightOperand.replace(ACTIVE, "").replace(":", "");
        if (version.isEmpty()) {
            version = null;
        }

        return Result.success(createPredicates(subType, version));
    }

    @NotNull
    private List<Predicate<VerifiableCredential>> createPredicates(String subtype, @Nullable String version) {
        var list = new ArrayList<Predicate<VerifiableCredential>>();
        list.add(new CredentialTypePredicate(CX_CREDENTIAL_NS, capitalize(subtype) + CREDENTIAL_LITERAL));

        if (version != null) {
            list.add(credential -> credential.getCredentialSubject().stream().anyMatch(cs -> version.equals(getClaimOrDefault(cs, CX_CREDENTIAL_NS, CONTRACT_VERSION_LITERAL, null))));
        }
        return list;
    }

    /**
     * Capitalizes (makes uppercase) the first character of a non-null input string.
     */
    private String capitalize(@NotNull String input) {
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }
}
