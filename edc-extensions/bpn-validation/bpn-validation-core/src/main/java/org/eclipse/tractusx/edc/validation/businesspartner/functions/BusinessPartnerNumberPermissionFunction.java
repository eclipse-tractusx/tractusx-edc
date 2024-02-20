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

import java.util.Optional;

import static java.lang.String.format;

/**
 * AtomicConstraintFunction to validate business partner numbers for edc permissions.
 */
public class BusinessPartnerNumberPermissionFunction implements AtomicConstraintFunction<Permission> {

    public BusinessPartnerNumberPermissionFunction() {
    }

    @Override
    public boolean evaluate(Operator operator, Object rightValue, Permission rule, PolicyContext context) {
        
        if (operator != Operator.EQ) {
            var message = format("As operator only 'EQ' is supported. Unsupported operator: '%s'", operator);
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
            context.reportProblem("Identity of the participant agent cannot be null: " + ParticipantAgent.class.getName());
            return false;
        }

        if ((rightValue instanceof String businessPartnerNumberStr)) {
            if (businessPartnerNumberStr.equals(identity)) {
                return true;
            } else {
                context.reportProblem("Identity of the participant not matching the expected one: " + businessPartnerNumberStr);
                return false;
            }
        } else {
            var message = format("Invalid right operand value: expected 'String' but got '%s'",
                    Optional.of(rightValue).map(Object::getClass).map(Class::getName).orElse(null));
            context.reportProblem(message);
            return false;
        }

    }
}
