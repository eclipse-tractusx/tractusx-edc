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

package org.eclipse.tractusx.edc.policy.cx.dismantler;

import org.eclipse.edc.identitytrust.model.VerifiableCredential;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.eclipse.tractusx.edc.policy.cx.common.AbstractDynamicConstraintFunction;
import org.eclipse.tractusx.edc.policy.cx.common.CredentialTypePredicate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;

import static org.eclipse.edc.policy.model.Operator.EQ;
import static org.eclipse.edc.policy.model.Operator.IN;
import static org.eclipse.edc.policy.model.Operator.IS_ANY_OF;
import static org.eclipse.edc.policy.model.Operator.IS_NONE_OF;
import static org.eclipse.edc.policy.model.Operator.NEQ;
import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.CredentialsNamespaces.CX_NS_1_0;

public class DismantlerConstraintFunction extends AbstractDynamicConstraintFunction {

    public static final String ALLOWED_VEHICLE_BRANDS = CX_NS_1_0 + "allowedVehicleBrands";
    private static final String DISMANTLER_LITERAL = "Dismantler";
    private static final String ALLOWED_ACTIVITIES = CX_NS_1_0 + "activityType";

    @SuppressWarnings({ "SuspiciousMethodCalls" })
    @Override
    public boolean evaluate(Object leftOperand, Operator operator, Object rightOperand, Permission permission, PolicyContext context) {
        Predicate<VerifiableCredential> predicate = c -> false;

        // make sure the ParticipantAgent is there
        var participantAgent = context.getContextData(ParticipantAgent.class);
        if (participantAgent == null) {
            context.reportProblem("Required PolicyContext data not found: " + ParticipantAgent.class.getName());
            return false;
        }

        // check if the participant agent contains the correct data
        var vcListResult = getCredentialList(participantAgent);
        if (vcListResult.failed()) { // couldn't extract credential list from agent
            context.reportProblem(vcListResult.getFailureDetail());
            return false;
        }

        if (leftOperand.equals(DISMANTLER_LITERAL)) { // only checks for presence
            if (!checkOperator(operator, context, EQUALITY_OPERATORS)) {
                return false;
            }
            if (!ACTIVE.equals(rightOperand)) {
                context.reportProblem("Right-operand must be equal to '%s', but was '%s'".formatted(ACTIVE, rightOperand));
                return false;
            }
            predicate = new CredentialTypePredicate(DISMANTLER_LITERAL + CREDENTIAL_LITERAL);
            if (operator == NEQ) {
                predicate = predicate.negate();
            }
        } else if (leftOperand.equals(DISMANTLER_LITERAL + ".activityType")) {
            if (rightOperand instanceof String) {
                if (!checkOperator(operator, context, EQUALITY_OPERATORS)) {
                    return false;
                }
            } else if (rightOperand instanceof Iterable<?>) {
                if (!checkOperator(operator, context, List.of(EQ, NEQ, IN, IS_ANY_OF, IS_NONE_OF))) {
                    return false;
                }
            } else {
                context.reportProblem("Invalid right-operand type: expected String or List, but got: %s".formatted(rightOperand.getClass().getName()));
                return false;
            }

            var allowedActivities = getList(rightOperand);
            // the filter predicate is determined by the operator
            predicate = credential -> credential.getCredentialSubject().stream().anyMatch(subject -> {
                var activitiesFromCredential = getList(subject.getClaims().getOrDefault(ALLOWED_ACTIVITIES, List.of()));
                return switch (operator) {
                    case EQ -> activitiesFromCredential.equals(allowedActivities);
                    case NEQ -> !activitiesFromCredential.equals(allowedActivities);
                    case IN ->
                            new HashSet<>(allowedActivities).containsAll(activitiesFromCredential); //IntelliJ says Hashset has better performance
                    case IS_ANY_OF -> !intersect(allowedActivities, activitiesFromCredential).isEmpty();
                    case IS_NONE_OF -> intersect(allowedActivities, activitiesFromCredential).isEmpty();
                    default -> false;
                };
            });

        } else if (leftOperand.equals(DISMANTLER_LITERAL + ".allowedBrands")) {
            if (rightOperand instanceof String) {
                if (!checkOperator(operator, context, EQUALITY_OPERATORS)) {
                    return false;
                }
            } else if (rightOperand instanceof Iterable<?>) {
                if (!checkOperator(operator, context, List.of(EQ, NEQ, IN, IS_ANY_OF, IS_NONE_OF))) {
                    return false;
                }
            } else {
                context.reportProblem("Invalid right-operand type: expected String or List, but got: %s".formatted(rightOperand.getClass().getName()));
                return false;
            }

            var allowedBrands = getList(rightOperand);

            // the filter predicate is determined by the operator
            predicate = credential -> credential.getCredentialSubject().stream().anyMatch(subject -> {
                var brandsFromCredential = getList(subject.getClaims().getOrDefault(ALLOWED_VEHICLE_BRANDS, List.of()));
                return switch (operator) {
                    case EQ -> brandsFromCredential.equals(allowedBrands);
                    case NEQ -> !brandsFromCredential.equals(allowedBrands);
                    case IN ->
                            new HashSet<>(allowedBrands).containsAll(brandsFromCredential); //IntelliJ says Hashset has better performance
                    case IS_ANY_OF -> !intersect(allowedBrands, brandsFromCredential).isEmpty();
                    case IS_NONE_OF -> intersect(allowedBrands, brandsFromCredential).isEmpty();
                    default -> false;
                };
            });
        } else {
            context.reportProblem("Invalid left-operand: must be 'Dismantler[.activityType | .allowedBrands ], but was '%s'".formatted(leftOperand));
            return false;
        }


        return !vcListResult.getContent().stream().filter(predicate)
                .toList().isEmpty();
    }

    @Override
    public boolean canHandle(Object leftOperand) {
        return leftOperand instanceof String && ((String) leftOperand).startsWith(DISMANTLER_LITERAL);
    }

    private List<?> intersect(List<?> list1, List<?> list2) {
        list1.retainAll(list2);
        return list1;
    }

    private List<?> getList(Object object) {
        if (object instanceof Iterable<?> iterable) {
            var list = new ArrayList<>();
            iterable.iterator().forEachRemaining(list::add);
            return list;
        }
        return List.of(object);
    }
}
