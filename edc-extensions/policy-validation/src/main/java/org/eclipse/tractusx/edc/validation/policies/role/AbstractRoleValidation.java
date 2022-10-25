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

package org.eclipse.tractusx.edc.validation.policies.role;

import java.util.Map;
import java.util.Objects;
import org.eclipse.dataspaceconnector.policy.model.Operator;
import org.eclipse.dataspaceconnector.spi.agent.ParticipantAgent;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.engine.PolicyContext;

/**
 * Abstract class for Role validation. This class may be inherited from the EDC policy enforcing
 * functions for duties, permissions and prohibitions.
 */
public abstract class AbstractRoleValidation {

  // Developer Note:
  // Problems reported to the policy context are not logged. Therefore, everything
  // that is reported to the policy context should be logged, too.

  private static final String FAIL_EVALUATION_BECAUSE_RIGHT_VALUE_NOT_STRING =
      "Failing evaluation because of invalid Role constraint. For operator 'EQ' right value must be of type 'String'. Unsupported type: '%s'";
  private static final String FAIL_EVALUATION_BECAUSE_UNSUPPORTED_OPERATOR =
      "Failing evaluation because of invalid Role constraint. As operator only 'EQ' is supported. Unsupported operator: '%s'";

  private final Monitor monitor;

  protected AbstractRoleValidation(Monitor monitor) {
    this.monitor = Objects.requireNonNull(monitor);
  }

  /** Name of the claim that contains the Role. */
  private static final String ROLE_CLAIM = "role";

  /**
   * Evaluation function to decide whether a claim belongs to a specific role.
   *
   * @param operator operator of the constraint
   * @param rightValue right value fo the constraint, that contains the defined role (e.g.
   *     dismantler)
   * @param policyContext context of the policy with claims
   * @return true if claims are from the constrained role
   */
  protected boolean evaluate(
      final Operator operator, final Object rightValue, final PolicyContext policyContext) {

    if (policyContext.hasProblems() && !policyContext.getProblems().isEmpty()) {
      String problems = String.join(", ", policyContext.getProblems());
      String message =
          String.format(
              "RoleValidation: Rejecting PolicyContext with problems. Problems: %s", problems);
      monitor.debug(message);
      return false;
    }

    final ParticipantAgent participantAgent = policyContext.getParticipantAgent();
    final Map<String, Object> claims = participantAgent.getClaims();

    if (!claims.containsKey(ROLE_CLAIM)) {
      return false;
    }

    Object roleClaimObject = claims.get(ROLE_CLAIM);
    String roleClaim = null;

    if (roleClaimObject instanceof String) {
      roleClaim = (String) roleClaimObject;
    }

    if (roleClaim == null || roleClaim.isEmpty()) {
      return false;
    }

    if (operator == Operator.EQ) {
      return isRole(roleClaim, rightValue, policyContext);
    } else {
      final String message = String.format(FAIL_EVALUATION_BECAUSE_UNSUPPORTED_OPERATOR, operator);
      monitor.warning(message);
      policyContext.reportProblem(message);
      return false;
    }
  }

  /**
   * @param roleClaim of the participant
   * @param role object
   * @return true if object is string and successfully evaluated against the claim
   */
  private boolean isRole(String roleClaim, Object role, PolicyContext policyContext) {
    if (role == null) {
      final String message = String.format(FAIL_EVALUATION_BECAUSE_RIGHT_VALUE_NOT_STRING, "null");
      monitor.warning(message);
      policyContext.reportProblem(message);
      return false;
    }
    if (!(role instanceof String)) {
      final String message =
          String.format(FAIL_EVALUATION_BECAUSE_RIGHT_VALUE_NOT_STRING, role.getClass().getName());
      monitor.warning(message);
      policyContext.reportProblem(message);
      return false;
    }

    return isCorrectRole(roleClaim, (String) role);
  }

  /**
   * @param roleClaim describing URL with the role
   * @param role of the constraint
   * @return true if claim is equal the role claim
   */
  private static boolean isCorrectRole(String roleClaim, String role) {
    return roleClaim.equals(role);
  }
}
