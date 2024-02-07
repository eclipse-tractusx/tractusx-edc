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

package org.eclipse.tractusx.edc.iatp.policy;

import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.agent.ParticipantAgent;

public class FrameworkCredentialEvaluationFunction extends BaseCredentialEvaluationFunction {

    public static final String CONTRACT_VERSION = "contractVersion";
    public static final String HOLDER_IDENTIFIER = "holderIdentifier";
    public static final String USE_CASE_TYPE = "useCaseType";
    public static final String CONTRACT_TEMPLATE = "contractTemplate";
    private final String usecase;
    private final String useCaseCredential;

    public FrameworkCredentialEvaluationFunction(String usecase) {
        this.usecase = usecase;
        this.useCaseCredential = "%sCredential".formatted(capitalize(usecase));
    }

    @Override
    public boolean evaluate(Operator operator, Object rightOperand, Permission permission, PolicyContext policyContext) {
        if (!operator.equals(Operator.EQ)) {
            policyContext.reportProblem("Cannot evaluate operator %s, only %s is supported".formatted(operator, Operator.EQ));
            return false;
        }
        if (!"active".equalsIgnoreCase(rightOperand.toString())) {
            policyContext.reportProblem("Use case credentials only support right operand 'active', but found '%s'".formatted(operator.toString()));
            return false;
        }
        var pa = policyContext.getContextData(ParticipantAgent.class);
        if (pa == null) {
            policyContext.reportProblem("ParticipantAgent not found on PolicyContext");
            return false;
        }

        var claims = pa.getClaims();

        var version = getClaim(String.class, CONTRACT_VERSION, claims);
        var holderIdentifier = getClaim(String.class, HOLDER_IDENTIFIER, claims);
        var contractTemplate = getClaim(String.class, CONTRACT_TEMPLATE, claims);
        var useCaseType = getClaim(String.class, USE_CASE_TYPE, claims);

        return version != null && holderIdentifier != null && contractTemplate != null && useCaseCredential.equals(useCaseType);
    }

    public String key() {
        return "FrameworkAgreement.%s".formatted(usecase);
    }

    private String capitalize(String input) {
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }
}
