/*
 * Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.edc.validation.businesspartner.functions;

import java.util.Map;
import java.util.Objects;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.eclipse.edc.spi.monitor.Monitor;

/**
 * Abstract class for BusinessPartnerNumber validation. This class may be inherited from the EDC
 * policy enforcing functions for duties, permissions and prohibitions.
 */
public abstract class AbstractBusinessPartnerValidation {

  // Developer Note:
  // Problems reported to the policy context are not logged. Therefore, everything
  // that is reported to the policy context should be logged, too.

  private static final String FAIL_EVALUATION_BECAUSE_RIGHT_VALUE_NOT_STRING =
      "Failing evaluation because of invalid BusinessPartnerNumber constraint. For operator 'EQ' right value must be of type 'String'. Unsupported type: '%s'";
  private static final String FAIL_EVALUATION_BECAUSE_UNSUPPORTED_OPERATOR =
      "Failing evaluation because of invalid BusinessPartnerNumber constraint. As operator only 'EQ' is supported. Unsupported operator: '%s'";

  private final Monitor monitor;

  protected AbstractBusinessPartnerValidation(Monitor monitor) {
    this.monitor = Objects.requireNonNull(monitor);
  }

  /**
   * Name of the claim that contains the Business Partner Number.
   *
   * <p><strong>Please note:</strong> At the time of writing (April 2022) the business partner
   * number is part of the 'referringConnector' claim in the IDS DAT token. This will probably
   * change for the next release.
   */
  private static final String REFERRING_CONNECTOR_CLAIM = "referringConnector";

  /**
   * Evaluation funtion to decide whether a claim belongs to a specific business partner.
   *
   * @param operator operator of the constraint
   * @param rightValue right value fo the constraint, that contains the business partner number
   *     (e.g. BPNLCDQ90000X42KU)
   * @param policyContext context of the policy with claims
   * @return true if claims are from the constrained business partner
   */
  protected boolean evaluate(
      final Operator operator, final Object rightValue, final PolicyContext policyContext) {

    if (policyContext.hasProblems() && !policyContext.getProblems().isEmpty()) {
      String problems = String.join(", ", policyContext.getProblems());
      String message =
          String.format(
              "BusinessPartnerNumberValidation: Rejecting PolicyContext with problems. Problems: %s",
              problems);
      monitor.debug(message);
      return false;
    }

    final ParticipantAgent participantAgent = policyContext.getParticipantAgent();
    final Map<String, Object> claims = participantAgent.getClaims();

    if (!claims.containsKey(REFERRING_CONNECTOR_CLAIM)) {
      return false;
    }

    Object referringConnectorClaimObject = claims.get(REFERRING_CONNECTOR_CLAIM);
    String referringConnectorClaim = null;

    if (referringConnectorClaimObject instanceof String) {
      referringConnectorClaim = (String) referringConnectorClaimObject;
    }

    if (referringConnectorClaim == null || referringConnectorClaim.isEmpty()) {
      return false;
    }

    if (operator == Operator.EQ) {
      return isBusinessPartnerNumber(referringConnectorClaim, rightValue, policyContext);
    } else {
      final String message = String.format(FAIL_EVALUATION_BECAUSE_UNSUPPORTED_OPERATOR, operator);
      monitor.warning(message);
      policyContext.reportProblem(message);
      return false;
    }
  }

  /**
   * @param referringConnectorClaim of the participant
   * @param businessPartnerNumber object
   * @return true if object is string and successfully evaluated against the claim
   */
  private boolean isBusinessPartnerNumber(
      String referringConnectorClaim, Object businessPartnerNumber, PolicyContext policyContext) {
    if (businessPartnerNumber == null) {
      final String message = String.format(FAIL_EVALUATION_BECAUSE_RIGHT_VALUE_NOT_STRING, "null");
      monitor.warning(message);
      policyContext.reportProblem(message);
      return false;
    }
    if (!(businessPartnerNumber instanceof String)) {
      final String message =
          String.format(
              FAIL_EVALUATION_BECAUSE_RIGHT_VALUE_NOT_STRING,
              businessPartnerNumber.getClass().getName());
      monitor.warning(message);
      policyContext.reportProblem(message);
      return false;
    }

    return isCorrectBusinessPartner(referringConnectorClaim, (String) businessPartnerNumber);
  }

  /**
   * At the time of writing (11. April 2022) the business partner number is part of the
   * 'referringConnector' claim, which contains a connector URL. As the CX projects are not further
   * aligned about the URL formatting, the enforcement can only be done by checking whether the URL
   * _contains_ the number. As this introduces some insecurities when validation business partner
   * numbers, this should be addresses in the long term.
   *
   * @param referringConnectorClaim describing URL with business partner number
   * @param businessPartnerNumber of the constraint
   * @return true if claim contains the business partner number
   */
  private static boolean isCorrectBusinessPartner(
      String referringConnectorClaim, String businessPartnerNumber) {
    return referringConnectorClaim.contains(businessPartnerNumber);
  }
}
