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

import org.eclipse.edc.identitytrust.model.VerifiableCredential;
import org.eclipse.edc.policy.engine.spi.DynamicAtomicConstraintFunction;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.agent.ParticipantAgent;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import static org.eclipse.tractusx.edc.iam.ssi.spi.jsonld.CredentialsNamespaces.CX_NS_1_0;


/**
 * Enforces a Framework Agreement constraint.
 * <p>
 * A policy constraints requiring a usecase framework credential take a left operand in the form:
 * <pre>FrameworkAgreement.[type]</pre>
 * <p>
 * The following example requires a client to present a sustainability credential:
 * <pre>
 * "constraint": {
 *     "leftOperand": "FrameworkAgreement.sustainability",
 *     "operator": "eq",
 *     "rightOperand": "active"
 * }
 * </pre>
 * <p>
 * NB: This function will be enabled in the 3.2 release.
 */
public class FrameworkAgreementConstraintFunction implements DynamicAtomicConstraintFunction<Permission> {
    public static final String CONTRACT_VERSION_PROPERTY = CX_NS_1_0 + "contractVersion";
    private static final String VC_CLAIM = "vc";
    private static final String ACTIVE = "active";
    /**
     * this regex matches legacy left-hand operands, where the use case type is directly encoded, e.g. FrameworkAgreement.pcf
     */
    private static final Pattern OLD_LEFT_OPERAND_REGEX = Pattern.compile("^(?i)(FrameworkAgreement\\.)(?<usecase>.+)$");
    /**
     * this regex matches new-style right-operands, which contain the use case type and an optional version, e.g. traceability:0.4.2
     */
    private static final Pattern RIGHT_OPERAND_REGEX = Pattern.compile("^(?<rightop>[a-zA-Z0-9]+)(:(?<version>.+))?$");
    private static final String FRAMEWORK_AGREEMENT_LITERAL = "FrameworkAgreement";
    private static final String CREDENTIAL_LITERAL = "Credential";

    public FrameworkAgreementConstraintFunction() {
    }

    @Override
    public boolean evaluate(Object leftValue, Operator operator, Object rightValue, Permission rule, PolicyContext context) {
        if (!leftValue.toString().startsWith(FRAMEWORK_AGREEMENT_LITERAL)) {
            context.reportProblem("Constraint left-operand must start with '%s' but was '%s'.".formatted(FRAMEWORK_AGREEMENT_LITERAL, leftValue));
            return false;
        }

        if (!Operator.EQ.equals(operator)) {
            context.reportProblem("Invalid operator: expected '%s', got '%s'.".formatted(Operator.EQ, operator));
            return false;
        }

        var participantAgent = context.getContextData(ParticipantAgent.class);
        if (participantAgent == null) {
            context.reportProblem("Required PolicyContext data not found: " + ParticipantAgent.class.getName());
            return false;
        }


        var vcListClaim = participantAgent.getClaims().get(VC_CLAIM);
        if (vcListClaim == null) {
            context.reportProblem("ParticipantAgent did not contain a '%s' claim.".formatted(VC_CLAIM));
            return false;
        }
        if (!(vcListClaim instanceof List)) {
            context.reportProblem("ParticipantAgent contains a '%s' claim, but the type is incorrect. Expected %s, got %s."
                    .formatted(VC_CLAIM, List.class.getName(), vcListClaim.getClass().getName()));
            return false;
        }
        var vcList = (List<VerifiableCredential>) vcListClaim;
        if (vcList.isEmpty()) {
            context.reportProblem("ParticipantAgent contains a '%s' claim but it did not contain any VerifiableCredentials.".formatted(VC_CLAIM));
            return false;
        }

        var rightOpMatcher = RIGHT_OPERAND_REGEX.matcher(rightValue.toString());
        var legacyMatcher = OLD_LEFT_OPERAND_REGEX.matcher(leftValue.toString());
        String subType; //this is the "usecase type", e.g. pcf, sustainability, etc.

        if (!rightOpMatcher.matches()) {
            context.reportProblem("Right-operand expected to contain (\"active\"|<subtype>)[:semver].");
            return false;
        }
        var version = rightOpMatcher.group("version"); // optional: a version string, may be null.

        if (legacyMatcher.matches()) { // "old" framework agreement statement format: "FrameworkAgreement.XYZ eq active:version"
            subType = legacyMatcher.group("usecase");
            var rightOp = rightOpMatcher.group("rightop");
            if (!ACTIVE.equals(rightOp)) {
                context.reportProblem("When the sub-type is encoded in the left-operand (here: %s), the right-operand must start with the 'active' keyword.".formatted(subType));
                return false;
            }
        } else if (FRAMEWORK_AGREEMENT_LITERAL.equals(leftValue.toString())) { // "new" framework agreement statement format: FrameworkAgreement eq xyz:version
            subType = rightOpMatcher.group("rightop");
        } else {
            context.reportProblem("Invalid left-operand: expected either FrameworkAgreement.<subtype> or FrameworkAgreement, found '%s'".formatted(leftValue));
            return false;
        }

        var credentials = filterCredential(vcList, subType, version);
        if (credentials.isEmpty()) {
            context.reportProblem("No credentials found that match the give Policy constraint: [%s %s %s]".formatted(leftValue.toString(), operator.toString(), rightValue.toString()));
            return false;
        }
        return true;

    }

    @Override
    public boolean canHandle(Object leftValue) {
        return leftValue instanceof String && leftValue.toString().startsWith(FRAMEWORK_AGREEMENT_LITERAL);
    }

    private Collection<VerifiableCredential> filterCredential(List<VerifiableCredential> vcList, String subType, String version) {
        return vcList.stream()
                .filter(vc -> vc.getTypes().contains(capitalize(subType) + CREDENTIAL_LITERAL))
                .filter(vc -> version == null || vc.getCredentialSubject().stream().anyMatch(cs -> cs.getClaims().get(CONTRACT_VERSION_PROPERTY).equals(version)))
                .toList();
    }

    private String capitalize(String input) {
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

}
