/*
 * Copyright (c) 2024 T-Systems International GmbH
 * Copyright (c) 2025 SAP SE
 * Copyright (c) 2026 Materna SE
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

package org.constructx.edc.policy.constructx.membership;

import org.constructx.edc.policy.constructx.common.AbstractDynamicCredentialConstraintFunction;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.tractusx.edc.core.utils.credentials.CredentialTypePredicate;

import static org.constructx.edc.policy.constructx.ConstructxPolicyConstants.CONSTRUCTX_CREDENTIAL_NS;
import static org.constructx.edc.policy.constructx.ConstructxPolicyConstants.CONSTRUCTX_POLICY_NS;


/**
 * This constraint function checks that a MembershipCredential is present in a list of {@link VerifiableCredential}
 * objects extracted from a {@link ParticipantAgent} which is expected to be present on the {@link ParticipantAgentPolicyContext}.
 */
public class MembershipCredentialConstraintFunction<C extends ParticipantAgentPolicyContext> extends AbstractDynamicCredentialConstraintFunction<C> {

    /**
     * key of the membership credential constraint
     *
     * @deprecated Use {@value CONSTRUCTX_MEMBERSHIP_LITERAL} instead.
     */
    @Deprecated(since = "0.0.4", forRemoval = true)
    public static final String MEMBERSHIP_LITERAL = "Membership";

    /**
     * key for constructx-membership credential constraint
     */
    public static final String CONSTRUCTX_MEMBERSHIP_LITERAL = "ConstructXMembership";

    private final Monitor monitor;

    public MembershipCredentialConstraintFunction(Monitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public boolean evaluate(Object leftOperand, Operator operator, Object rightOperand, Permission permission, C context) {
        if (!ACTIVE.equals(rightOperand)) {
            context.reportProblem("Right-operand must be equal to '%s', but was '%s'".formatted(ACTIVE, rightOperand));
            return false;
        }

        if ((CONSTRUCTX_POLICY_NS + MEMBERSHIP_LITERAL).equals(leftOperand)) {
            monitor.warning("The %s%s Policy is deprecated since version 0.0.4 and will be removed in future releases. Please use %s%s Policy instead."
                    .formatted(CONSTRUCTX_POLICY_NS, MEMBERSHIP_LITERAL, CONSTRUCTX_POLICY_NS, CONSTRUCTX_MEMBERSHIP_LITERAL));
        }

        // make sure the ParticipantAgent is there
        var participantAgent = extractParticipantAgent(context);
        if (participantAgent.failed()) {
            context.reportProblem(participantAgent.getFailureDetail());
            return false;
        }

        var credentialResult = getCredentialList(participantAgent.getContent());
        if (credentialResult.failed()) {
            context.reportProblem(credentialResult.getFailureDetail());
            return false;
        }
        return credentialResult.getContent()
                .stream()
                .anyMatch(new CredentialTypePredicate(CONSTRUCTX_CREDENTIAL_NS, CONSTRUCTX_MEMBERSHIP_LITERAL + CREDENTIAL_LITERAL)
                        .or(new CredentialTypePredicate(CONSTRUCTX_CREDENTIAL_NS, MEMBERSHIP_LITERAL + CREDENTIAL_LITERAL)));
    }

    @Override
    public boolean canHandle(Object leftOperand) {
        return (CONSTRUCTX_POLICY_NS + CONSTRUCTX_MEMBERSHIP_LITERAL).equals(leftOperand) || (CONSTRUCTX_POLICY_NS + MEMBERSHIP_LITERAL).equals(leftOperand);
    }
}
