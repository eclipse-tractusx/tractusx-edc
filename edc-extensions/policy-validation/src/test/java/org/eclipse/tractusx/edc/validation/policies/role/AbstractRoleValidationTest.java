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

package org.eclipse.tractusx.edc.validation.policies.role;

import java.util.Collections;
import java.util.List;
import org.eclipse.dataspaceconnector.policy.model.Operator;
import org.eclipse.dataspaceconnector.spi.agent.ParticipantAgent;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.engine.PolicyContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;

class AbstractRoleValidationTest {

  private AbstractRoleValidation validation;

  // mocks
  private Monitor monitor;
  private PolicyContext policyContext;
  private ParticipantAgent participantAgent;

  @BeforeEach
  void BeforeEach() {
    this.monitor = Mockito.mock(Monitor.class);
    this.policyContext = Mockito.mock(PolicyContext.class);
    this.participantAgent = Mockito.mock(ParticipantAgent.class);

    Mockito.when(policyContext.getParticipantAgent()).thenReturn(participantAgent);

    validation = new AbstractRoleValidation(monitor) {};
  }

  @ParameterizedTest
  @EnumSource(Operator.class)
  void testFailsOnUnsupportedOperations(Operator operator) {

    if (operator == Operator.EQ) { // only allowed operator
      return;
    }

    // prepare
    prepareContextProblems(null);
    prepareRoleClaim("yes");

    // invoke & assert
    Assertions.assertFalse(validation.evaluate(operator, "Dismantler", policyContext));
  }

  @Test
  void testFailsOnUnsupportedRightValue() {

    // prepare
    prepareContextProblems(null);
    prepareRoleClaim("yes");

    // invoke & assert
    Assertions.assertFalse(validation.evaluate(Operator.EQ, 1, policyContext));
  }

  @Test
  void testValidationFailsWhenClaimMissing() {

    // prepare
    prepareContextProblems(null);

    // invoke
    final boolean isValid = validation.evaluate(Operator.EQ, "Dismantler", policyContext);

    // assert
    Assertions.assertFalse(isValid);
  }

  @Test
  void testValidationSucceedsWhenClaimEqualsValue() {

    // prepare
    prepareContextProblems(null);

    // prepare equals
    prepareRoleClaim("Dismantler");
    final boolean isEqualsTrue = validation.evaluate(Operator.EQ, "Dismantler", policyContext);

    // assert
    Assertions.assertTrue(isEqualsTrue);
  }

  @Test
  void testValidationWhenParticipantHasProblems() {

    // prepare
    prepareContextProblems(Collections.singletonList("big problem"));
    prepareRoleClaim("Dismantler");

    // invoke
    final boolean isValid = validation.evaluate(Operator.EQ, "Dismantler", policyContext);

    Assertions.assertFalse(isValid);
  }

  private void prepareContextProblems(List<String> problems) {
    Mockito.when(policyContext.getProblems()).thenReturn(problems);

    if (problems == null || problems.isEmpty()) {
      Mockito.when(policyContext.hasProblems()).thenReturn(false);
    } else {
      Mockito.when(policyContext.hasProblems()).thenReturn(true);
    }
  }

  private void prepareRoleClaim(String role) {
    Mockito.when(participantAgent.getClaims()).thenReturn(Collections.singletonMap("role", role));
  }
}
