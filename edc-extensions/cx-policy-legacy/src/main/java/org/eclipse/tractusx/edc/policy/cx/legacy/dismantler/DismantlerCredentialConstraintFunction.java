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

package org.eclipse.tractusx.edc.policy.cx.legacy.dismantler;

import jakarta.json.JsonObject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.tractusx.edc.core.utils.credentials.CredentialTypePredicate;
import org.eclipse.tractusx.edc.policy.cx.legacy.common.AbstractDynamicCredentialConstraintFunction;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;

import static org.eclipse.edc.policy.model.Operator.EQ;
import static org.eclipse.edc.policy.model.Operator.IN;
import static org.eclipse.edc.policy.model.Operator.IS_ANY_OF;
import static org.eclipse.edc.policy.model.Operator.IS_NONE_OF;
import static org.eclipse.edc.policy.model.Operator.NEQ;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.CX_CREDENTIAL_NS;
import static org.eclipse.tractusx.edc.edr.spi.CoreConstants.CX_POLICY_NS;

/**
 * Enforces a Dismantler constraint. This function can check for these properties:
 * <ul>
 *     <li>presence: whether a Dismantler credential is present or not</li>
 *     <li>activityType: whether an existing DismantlerCredential permits the activity types required by the constraint</li>
 *     <li>allowedBrands: whether an existing DismantlerCredential permits the vehicle brands required by the constraint</li>
 * </ul>
 */
public class DismantlerCredentialConstraintFunction<C extends ParticipantAgentPolicyContext> extends AbstractDynamicCredentialConstraintFunction<C> {

    public static final String ALLOWED_VEHICLE_BRANDS_LITERAL = "allowedVehicleBrands";
    public static final String DISMANTLER_LITERAL = "Dismantler";
    // allows to encode multiple values in a single string in the right-operand. Policies don't handle list-type right-operands well.
    public static final String RIGHT_OPERAND_LIST_SEPARATOR = ",";
    private static final String ALLOWED_ACTIVITIES_LITERAL = "activityType";

    @Override
    public boolean evaluate(Object leftOperand, Operator operator, Object rightOperand, Permission permission, C context) {
        Predicate<VerifiableCredential> predicate;

        var participantAgent = context.participantAgent();

        // check if the participant agent contains the correct data
        var vcListResult = getCredentialList(participantAgent);
        if (vcListResult.failed()) { // couldn't extract credential list from agent
            context.reportProblem(vcListResult.getFailureDetail());
            return false;
        }

        // always filter for DismantlerCredential type
        predicate = new CredentialTypePredicate(CX_CREDENTIAL_NS, DISMANTLER_LITERAL + CREDENTIAL_LITERAL);


        if (rightOperand.toString().contains(RIGHT_OPERAND_LIST_SEPARATOR)) {
            rightOperand = getList(rightOperand);
        }

        if (leftOperand.equals(CX_POLICY_NS + DISMANTLER_LITERAL)) { // only checks for presence
            if (!checkOperator(operator, context, EQUALITY_OPERATORS)) {
                return false;
            }
            if (!ACTIVE.equals(rightOperand)) {
                context.reportProblem("Right-operand must be equal to '%s', but was '%s'".formatted(ACTIVE, rightOperand));
                return false;
            }
            if (operator == NEQ) {
                predicate = predicate.negate();
            }

        } else if (leftOperand.equals(CX_POLICY_NS + DISMANTLER_LITERAL + ".activityType")) {
            if (hasInvalidOperand(operator, rightOperand, context)) return false;
            predicate = predicate.and(getCredentialPredicate(ALLOWED_ACTIVITIES_LITERAL, operator, rightOperand));
        } else if (leftOperand.equals(CX_POLICY_NS + DISMANTLER_LITERAL + ".allowedBrands")) {
            if (hasInvalidOperand(operator, rightOperand, context)) return false;
            predicate = predicate.and(getCredentialPredicate(ALLOWED_VEHICLE_BRANDS_LITERAL, operator, rightOperand));
        } else {
            context.reportProblem("Invalid left-operand: must be 'Dismantler[.activityType | .allowedBrands ], but was '%s'".formatted(leftOperand));
            return false;
        }

        return vcListResult.getContent().stream().anyMatch(predicate);
    }

    @Override
    public boolean canHandle(Object leftOperand) {
        return leftOperand instanceof String && ((String) leftOperand).startsWith(CX_POLICY_NS + DISMANTLER_LITERAL);
    }

    /**
     * Creates a {@link Predicate} based on the {@code rightOperand} that tests whether whatever property is extracted from the {@link VerifiableCredential}
     * is valid, according to the operator. For example {@link Operator#IS_ALL_OF} would check that the list from the constraint (= rightOperand) intersects with the list
     * stored in the claim identified by {@code credentialSubjectProperty} in the {@link VerifiableCredential#getCredentialSubject()}.
     *
     * @param credentialSubjectProperty The name of the claim to be extracted from the {@link VerifiableCredential#getCredentialSubject()}
     * @param operator                  the operator
     * @param rightOperand              The constraint value (i.e. policy expression right-operand)
     * @return A predicate that tests a {@link VerifiableCredential} for the constraint
     */
    @NotNull
    private Predicate<VerifiableCredential> getCredentialPredicate(String credentialSubjectProperty, Operator operator, Object rightOperand) {
        var allowedValues = getList(rightOperand);
        // the filter predicate is determined by the operator
        return credential -> credential.getCredentialSubject().stream().anyMatch(subject -> {
            var claimsFromCredential = getList(getClaimOrDefault(subject, CX_CREDENTIAL_NS, credentialSubjectProperty, List.of()));
            return switch (operator) {
                case EQ -> claimsFromCredential.equals(allowedValues);
                case NEQ -> !claimsFromCredential.equals(allowedValues);
                case IN ->
                        new HashSet<>(allowedValues).containsAll(claimsFromCredential); //IntelliJ says Hashset has better performance
                case IS_ANY_OF -> !intersect(allowedValues, claimsFromCredential).isEmpty();
                case IS_NONE_OF -> intersect(allowedValues, claimsFromCredential).isEmpty();
                default -> false;
            };
        });
    }

    /**
     * Checks whether {@code operator} is valid in the context of {@code rightOperand}. In practice, this means that if {@code rightOperand} is a String, it checks for {@link AbstractDynamicCredentialConstraintFunction#EQUALITY_OPERATORS},
     * and if its list type, it checks for {@code List.of(EQ, NEQ, IN, IS_ANY_OF, IS_NONE_OF)}
     */
    private boolean hasInvalidOperand(Operator operator, Object rightOperand, PolicyContext context) {
        if (rightOperand instanceof String) {
            return !checkOperator(operator, context, EQUALITY_OPERATORS);
        } else if (rightOperand instanceof Iterable<?>) {
            return !checkOperator(operator, context, List.of(EQ, NEQ, IN, IS_ANY_OF, IS_NONE_OF));
        } else {
            context.reportProblem("Invalid right-operand type: expected String or List, but received: %s".formatted(rightOperand.getClass().getName()));
            return true;
        }
    }

    /**
     * Checks whether two lists "intersect", i.e. that at least one element is found in both lists.
     * Caution: {@code list1} may be modified!
     */
    private List<?> intersect(List<?> list1, List<?> list2) {
        list1.retainAll(list2);
        return list1;
    }

    private List<?> getList(Object object) {
        if (object instanceof Iterable<?> iterable) {
            var list = new ArrayList<>();

            iterable.iterator().forEachRemaining(element -> {
                if (element instanceof JsonObject jo) { // workaround: lists in policy right-operands are not properly deserialized
                    list.add(jo.getString("@value"));
                } else {
                    list.add(element);
                }
            });
            return list;
        }
        return List.of(object.toString().split(RIGHT_OPERAND_LIST_SEPARATOR)); // in case multiple values are encoded in a single string
    }
}
