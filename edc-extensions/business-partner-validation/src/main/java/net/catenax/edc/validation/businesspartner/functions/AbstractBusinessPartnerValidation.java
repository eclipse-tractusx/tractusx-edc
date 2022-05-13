/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Initial API and Implementation
 *
 */

package net.catenax.edc.validation.businesspartner.functions;

import java.util.Map;
import java.util.Objects;
import org.eclipse.dataspaceconnector.policy.model.Operator;
import org.eclipse.dataspaceconnector.spi.agent.ParticipantAgent;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.PolicyContext;

/**
 * Abstract class for BusinessPartnerNumber validation. This class may be inherited from the EDC
 * policy enforcing functions for duties, permissions and prohibitions.
 */
public abstract class AbstractBusinessPartnerValidation {

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
  private static final String BUSINESS_PARTNER_NUMBER_CLAIM_KEY = "referringConnector";

  /**
   * Evaluation funtion to decide whether a claim belongs to a specific business partner.
   *
   * @param operator operator of the constraint
   * @param rightValue right value fo the constraint, that contains the business partner number
   *     (e.g. BPNLCDQ90000X42KU)
   * @param claims claims of the participant / business partner
   * @return true if claims are from the constrained business partner
   */
  protected boolean evaluate(
      final Operator operator, final Object rightValue, final PolicyContext policyContext) {

    if (policyContext.hasProblems() && policyContext.getProblems().size() > 0) {
      String problems = String.join(", ", policyContext.getProblems());
      String logMessage =
          String.format(
              "BusinessPartnerNumberValidation: Rejecting PolicyContext with problems. Problems: %s",
              problems);
      monitor.debug(logMessage);
      return false;
    }

    final ParticipantAgent participantAgent = policyContext.getParticipantAgent();
    final Map<String, String> claims = participantAgent.getClaims();
    if (!claims.containsKey(BUSINESS_PARTNER_NUMBER_CLAIM_KEY)) {
      return false;
    }

    if (operator != Operator.EQ) {
      throw new UnsupportedOperationException(
          "Operator for BusinessPartnerNumber must always be 'EQ'");
    }

    if (!(rightValue instanceof String)) {
      throw new UnsupportedOperationException(
          "Right value of BusinessPartnerNumber constraint must be of type 'String'");
    }

    String claimValue = claims.get(BUSINESS_PARTNER_NUMBER_CLAIM_KEY);

    // At the time of writing the business partner number is part of the
    // 'referingConnector' claim, which contains a connector URL.
    // As the CX projects are not further alligned about the URL formatting, the
    // enforcement can only be done by checking whether the URL _contains_ the
    // number.
    return claimValue.contains((String) rightValue);
  }
}
