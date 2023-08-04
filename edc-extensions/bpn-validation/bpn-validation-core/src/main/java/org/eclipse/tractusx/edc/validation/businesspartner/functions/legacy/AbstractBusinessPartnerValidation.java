/*
 *
 *   Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft
 *
 *   See the NOTICE file(s) distributed with this work for additional
 *   information regarding copyright ownership.
 *
 *   This program and the accompanying materials are made available under the
 *   terms of the Apache License, Version 2.0 which is available at
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *   WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *   License for the specific language governing permissions and limitations
 *   under the License.
 *
 *   SPDX-License-Identifier: Apache-2.0
 *
 */

package org.eclipse.tractusx.edc.validation.businesspartner.functions.legacy;

import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.eclipse.edc.spi.monitor.Monitor;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static java.lang.String.format;

/**
 * Abstract class for BusinessPartnerNumber validation. This class may be inherited from the EDC
 * policy enforcing functions for duties, permissions and prohibitions.
 *
 * @deprecated Please use {@code BusinessPartnerGroupFunction} instead
 */
@Deprecated(forRemoval = true, since = "0.5.0")
public abstract class AbstractBusinessPartnerValidation {

    // Developer Note:
    // Problems reported to the policy context are not logged. Therefore, everything
    // that is reported to the policy context should be logged, too.

    private static final String FAIL_EVALUATION_BECAUSE_RIGHT_VALUE_NOT_STRING =
            "Failing evaluation because of invalid BusinessPartnerNumber constraint. For operator 'EQ' right value must be of type 'String'. Unsupported type: '%s'";
    private static final String FAIL_EVALUATION_BECAUSE_UNSUPPORTED_OPERATOR =
            "Failing evaluation because of invalid BusinessPartnerNumber constraint. As operator only 'EQ' is supported. Unsupported operator: '%s'";
    /**
     * Name of the claim that contains the Business Partner Number.
     *
     * <p><strong>Please note:</strong> At the time of writing (April 2022) the business partner
     * number is part of the 'referringConnector' claim in the IDS DAT token. This will probably
     * change for the next release.
     */
    private static final String REFERRING_CONNECTOR_CLAIM = "referringConnector";
    private final Monitor monitor;
    private final boolean logAgreementEvaluation;

    protected AbstractBusinessPartnerValidation(Monitor monitor, boolean logAgreementEvaluation) {
        this.monitor = Objects.requireNonNull(monitor);
        this.logAgreementEvaluation = logAgreementEvaluation;
    }

    /**
     * At the time of writing (11. April 2022) the business partner number is part of the
     * 'referringConnector' claim, which contains a connector URL. As the CX projects are not further
     * aligned about the URL formatting, the enforcement can only be done by checking whether the URL
     * _contains_ the number. As this introduces some insecurities when validation business partner
     * numbers, this should be addresses in the long term.
     *
     * @param referringConnectorClaim describing URL with business partner number
     * @param businessPartnerNumber   of the constraint
     * @return true if claim contains the business partner number
     */
    private static boolean isCorrectBusinessPartner(String referringConnectorClaim, String businessPartnerNumber) {
        return referringConnectorClaim.contains(businessPartnerNumber);
    }

    public boolean isLogAgreementEvaluation() {
        return logAgreementEvaluation;
    }

    /**
     * Evaluation funtion to decide whether a claim belongs to a specific business partner.
     *
     * @param operator      operator of the constraint
     * @param rightValue    right value fo the constraint, that contains the business partner number
     *                      (e.g. BPNLCDQ90000X42KU)
     * @param policyContext context of the policy with claims
     * @return true if claims are from the constrained business partner
     */
    public boolean evaluate(Operator operator, Object rightValue, PolicyContext policyContext) {

        monitor.warning("This policy evaluation function (class [%s]) was deprecated and is scheduled for removal in version 0.6.0 of Tractus-X EDC".formatted(getClass().getSimpleName()));

        if (policyContext.hasProblems() && !policyContext.getProblems().isEmpty()) {
            var problems = String.join(", ", policyContext.getProblems());
            var message =
                    format(
                            "BusinessPartnerNumberValidation: Rejecting PolicyContext with problems. Problems: %s",
                            problems);
            monitor.debug(message);
            return false;
        }

        var participantAgent = policyContext.getContextData(ParticipantAgent.class);

        if (participantAgent == null) {
            return false;
        }
        var referringConnectorClaim = getReferringConnectorClaim(participantAgent);

        if (referringConnectorClaim == null || referringConnectorClaim.isEmpty()) {
            return false;
        }

        if (operator == Operator.EQ) {
            return isBusinessPartnerNumber(referringConnectorClaim, rightValue, policyContext);
        } else {
            var message = format(FAIL_EVALUATION_BECAUSE_UNSUPPORTED_OPERATOR, operator);
            monitor.warning(message);
            policyContext.reportProblem(message);
            return false;
        }
    }

    @Nullable
    private String getReferringConnectorClaim(ParticipantAgent participantAgent) {
        String referringConnectorClaim = null;
        var claims = participantAgent.getClaims();

        var referringConnectorClaimObject = claims.get(REFERRING_CONNECTOR_CLAIM);

        if (referringConnectorClaimObject instanceof String) {
            referringConnectorClaim = (String) referringConnectorClaimObject;
        }
        if (referringConnectorClaim == null) {
            referringConnectorClaim = participantAgent.getIdentity();
        }

        return referringConnectorClaim;
    }

    private boolean isBusinessPartnerNumber(String referringConnectorClaim, Object businessPartnerNumber, PolicyContext policyContext) {
        if (businessPartnerNumber == null) {
            var message = format(FAIL_EVALUATION_BECAUSE_RIGHT_VALUE_NOT_STRING, "null");
            monitor.warning(message);
            policyContext.reportProblem(message);
            return false;
        }
        if (!(businessPartnerNumber instanceof String businessPartnerNumberStr)) {
            var message =
                    format(
                            FAIL_EVALUATION_BECAUSE_RIGHT_VALUE_NOT_STRING,
                            businessPartnerNumber.getClass().getName());
            monitor.warning(message);
            policyContext.reportProblem(message);
            return false;
        }

        var agreement = policyContext.getContextData(ContractAgreement.class);
        var isCorrectBusinessPartner = isCorrectBusinessPartner(referringConnectorClaim, businessPartnerNumberStr);

        if (agreement != null && logAgreementEvaluation) {
            monitor.info(format("Evaluated policy access for referringConnectorClaim: %s and contract id: %s with result: %s", referringConnectorClaim, agreement.getId(), isCorrectBusinessPartner));
        }
        return isCorrectBusinessPartner;
    }
}
