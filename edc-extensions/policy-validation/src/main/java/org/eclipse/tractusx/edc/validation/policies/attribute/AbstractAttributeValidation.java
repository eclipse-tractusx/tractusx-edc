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
 *       Mercedes-Benz Tech Innovation GmbH - Right value of constraint can now contain iterable of BPNs
 *
 */

package org.eclipse.tractusx.edc.validation.policies.attribute;

import java.util.Map;
import java.util.Objects;
import org.eclipse.dataspaceconnector.policy.model.Operator;
import org.eclipse.dataspaceconnector.spi.agent.ParticipantAgent;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.engine.PolicyContext;

/**
 * Abstract class for Attribute validation. This class may be inherited from the EDC policy
 * enforcing functions for duties, permissions and prohibitions.
 */
public abstract class AbstractAttributeValidation {

  // Developer Note:
  // Problems reported to the policy context are not logged. Therefore, everything
  // that is reported to the policy context should be logged, too.

  private static final String FAIL_EVALUATION_BECAUSE_RIGHT_VALUE_NOT_STRING =
      "Failing evaluation because of invalid Attribute constraint. For operator 'EQ' right value must be of type 'String'. Unsupported type: '%s'";
  private static final String FAIL_EVALUATION_BECAUSE_UNSUPPORTED_OPERATOR =
      "Failing evaluation because of invalid Attribute constraint. As operator only 'EQ' is supported. Unsupported operator: '%s'";

  private final Monitor monitor;

  protected AbstractAttributeValidation(Monitor monitor) {
    this.monitor = Objects.requireNonNull(monitor);
  }

  /** Name of the claim that contains the Attribute. */
  private static final String ATTRIBUTE_CLAIM = "attribute";

  /**
   * Evaluation function to decide whether a claim belongs to a specific Attribute.
   *
   * @param operator operator of the constraint
   * @param rightValue right value fo the constraint, that contains the Attribute (e.g.
   *     ISO-CERTIFICATED)
   * @param policyContext context of the policy with claims
   * @return true if claims are from the constrained attribute partner
   */
  protected boolean evaluate(
      final Operator operator, final Object rightValue, final PolicyContext policyContext) {

    if (policyContext.hasProblems() && !policyContext.getProblems().isEmpty()) {
      String problems = String.join(", ", policyContext.getProblems());
      String message =
          String.format(
              "AttributeValidation: Rejecting PolicyContext with problems. Problems: %s", problems);
      monitor.debug(message);
      return false;
    }

    final ParticipantAgent participantAgent = policyContext.getParticipantAgent();
    final Map<String, Object> claims = participantAgent.getClaims();

    if (!claims.containsKey(ATTRIBUTE_CLAIM)) {
      return false;
    }

    Object attributeClaimObject = claims.get(ATTRIBUTE_CLAIM);
    String attributeClaim = null;

    if (attributeClaimObject instanceof String) {
      attributeClaim = (String) attributeClaimObject;
    }

    if (attributeClaim == null || attributeClaim.isEmpty()) {
      return false;
    }

    if (operator == Operator.EQ) {
      String[] attributes = attributeClaim.split(",");
      for(String attribute : attributes){
        if(isAttributeValue(attribute, rightValue, policyContext)){
          return true;
        }
      }
      return false;
    } else {
      final String message = String.format(FAIL_EVALUATION_BECAUSE_UNSUPPORTED_OPERATOR, operator);
      monitor.warning(message);
      policyContext.reportProblem(message);
      return false;
    }
  }

  /**
   * @param attributeClaim of the participant
   * @param attribute object
   * @return true if object is string and successfully evaluated against the claim
   */
  private boolean isAttributeValue(
      String attributeClaim, Object attribute, PolicyContext policyContext) {
    if (attribute == null) {
      final String message = String.format(FAIL_EVALUATION_BECAUSE_RIGHT_VALUE_NOT_STRING, "null");
      monitor.warning(message);
      policyContext.reportProblem(message);
      return false;
    }
    if (!(attribute instanceof String)) {
      final String message =
          String.format(
              FAIL_EVALUATION_BECAUSE_RIGHT_VALUE_NOT_STRING, attribute.getClass().getName());
      monitor.warning(message);
      policyContext.reportProblem(message);
      return false;
    }
    return isCorrectAttribute(attributeClaim, (String) attribute);
  }

  /**
   * @param attributeClaim describing URL with attribute
   * @param attribute of the constraint
   * @return true if claim is equal the attribute claim
   */
  private static boolean isCorrectAttribute(String attributeClaim, String attribute) {
    return attributeClaim.equals(attribute);
  }
}
