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

package org.eclipse.tractusx.edc.validation.businesspartner.functions;

import org.eclipse.edc.policy.engine.spi.AtomicConstraintFunction;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.agent.ParticipantAgent;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * AtomicConstraintFunction to validate business partner numbers for edc permissions.
 */
public class BusinessPartnerNumberPermissionFunction implements AtomicConstraintFunction<Permission> {

    private static final List<Operator> SUPPORTED_OPERATORS = Arrays.asList(
            Operator.EQ,
            Operator.IN,
            Operator.NEQ,
            Operator.IS_ANY_OF,
            Operator.IS_A,
            Operator.IS_NONE_OF,
            Operator.IS_ALL_OF,
            Operator.HAS_PART
    );
    private static final List<Operator> SCALAR_OPERATORS = Arrays.asList(
            Operator.EQ,
            Operator.NEQ,
            Operator.HAS_PART
    );

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public boolean evaluate(Operator operator, Object rightValue, Permission rule, PolicyContext context) {

        if (!SUPPORTED_OPERATORS.contains(operator)) {
            var message = "Operator %s is not supported. Supported operators: %s".formatted(operator, SUPPORTED_OPERATORS);
            context.reportProblem(message);
            return false;
        }

        var participantAgent = context.getContextData(ParticipantAgent.class);
        if (participantAgent == null) {
            context.reportProblem("Required PolicyContext data not found: " + ParticipantAgent.class.getName());
            return false;
        }

        var identity = participantAgent.getIdentity();
        if (identity == null) {
            context.reportProblem("Identity of the participant agent cannot be null");
            return false;
        }

        if (SCALAR_OPERATORS.contains(operator)) {
            if (rightValue instanceof String businessPartnerNumberStr) {
                return switch (operator) {
                    case EQ -> businessPartnerNumberStr.equals(identity);
                    case NEQ -> !businessPartnerNumberStr.equals(identity);
                    case HAS_PART -> identity.contains(businessPartnerNumberStr);
                    default -> false;
                };
            }
            context.reportProblem("Invalid right-value: operator '%s' requires a 'String' but got a '%s'".formatted(operator, Optional.of(rightValue).map(Object::getClass).map(Class::getName).orElse(null)));
        } else {
            if ((rightValue instanceof List numbers)) {
                return switch (operator) {
                    case IN, IS_A, IS_ANY_OF -> numbers.contains(identity);
                    case IS_ALL_OF -> numbers.stream().allMatch(o -> o.equals(identity));
                    case IS_NONE_OF -> !numbers.contains(identity);
                    default -> false;
                };
            }
            context.reportProblem("Invalid right-value: operator '%s' requires a 'List' but got a '%s'".formatted(operator, Optional.of(rightValue).map(Object::getClass).map(Class::getName).orElse(null)));
        }
        return false;
    }
}
