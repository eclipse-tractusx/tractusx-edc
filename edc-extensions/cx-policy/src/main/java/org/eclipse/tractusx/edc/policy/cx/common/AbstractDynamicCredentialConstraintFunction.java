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

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.DynamicAtomicConstraintRuleFunction;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.result.Result;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * This is a base class for dynamically bound Tractus-X constraint evaluation functions that implements some basic common functionality and defines some
 * common constants
 */
public abstract class AbstractDynamicCredentialConstraintFunction<C extends ParticipantAgentPolicyContext> implements DynamicAtomicConstraintRuleFunction<Permission, C> {
    public static final String VC_CLAIM = "vc";
    public static final String ACTIVE = "active";
    public static final String CREDENTIAL_LITERAL = "Credential";
    protected static final Collection<Operator> EQUALITY_OPERATORS = List.of(Operator.EQ, Operator.NEQ);

    protected boolean checkOperator(Operator actual, PolicyContext context, Collection<Operator> expectedOperators) {
        if (!expectedOperators.contains(actual)) {
            context.reportProblem("Invalid operator: this constraint only allows the following operators: %s, but received '%s'.".formatted(EQUALITY_OPERATORS, actual));
            return false;
        }
        return true;
    }

    /**
     * Extracts a {@link List} of {@link VerifiableCredential} objects from the {@link ParticipantAgent}. Credentials must be
     * stored in the agent's claims map using the "vc" key.
     */
    protected Result<List<VerifiableCredential>> getCredentialList(ParticipantAgent agent) {
        var vcListClaim = agent.getClaims().get(VC_CLAIM);

        if (vcListClaim == null) {
            return Result.failure("ParticipantAgent did not contain a '%s' claim.".formatted(VC_CLAIM));
        }
        if (!(vcListClaim instanceof List)) {
            return Result.failure("ParticipantAgent contains a '%s' claim, but the type is incorrect. Expected %s, received %s.".formatted(VC_CLAIM, List.class.getName(), vcListClaim.getClass().getName()));
        }
        var vcList = (List<VerifiableCredential>) vcListClaim;
        if (vcList.isEmpty()) {
            return Result.failure("ParticipantAgent contains a '%s' claim but it did not contain any VerifiableCredentials.".formatted(VC_CLAIM));
        }
        return Result.success(vcList);
    }

    protected Object getClaimOrDefault(CredentialSubject subject, String namespace, String property, Object def) {
        return Optional.ofNullable(subject.getClaims().get(namespace + property))
                .or(() -> Optional.ofNullable(subject.getClaims().get(property)))
                .orElse(def);
    }
}
