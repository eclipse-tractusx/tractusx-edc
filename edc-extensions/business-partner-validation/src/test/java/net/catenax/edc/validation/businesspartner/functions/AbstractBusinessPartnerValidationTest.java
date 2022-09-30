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

class AbstractBusinessPartnerValidationTest {

  private AbstractBusinessPartnerValidation validation;

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

    validation = new AbstractBusinessPartnerValidation(monitor) {};
  }

  @ParameterizedTest
  @EnumSource(Operator.class)
  void testFailsOnUnsupportedOperations(Operator operator) {

    if (operator == Operator.EQ || operator == Operator.IN) { // only allowed operator
      return;
    }

    // prepare
    prepareContextProblems(null);
    prepareBusinessPartnerClaim("yes");

    // invoke & assert
    Assertions.assertFalse(validation.evaluate(operator, "foo", policyContext));
  }

  @Test
  void testFailsOnUnsupportedRightValue() {

    // prepare
    prepareContextProblems(null);
    prepareBusinessPartnerClaim("yes");

    // invoke & assert
    Assertions.assertFalse(validation.evaluate(Operator.EQ, 1, policyContext));
    Assertions.assertFalse(validation.evaluate(Operator.IN, "foo", policyContext));
  }

  @Test
  void testValidationFailsWhenClaimMissing() {

    // prepare
    prepareContextProblems(null);

    // invoke
    final boolean isValid = validation.evaluate(Operator.EQ, "foo", policyContext);

    // assert
    Assertions.assertFalse(isValid);
  }

  @Test
  void testValidationSucceedsWhenClaimContainsValue() {

    // prepare
    prepareContextProblems(null);

    // prepare equals
    prepareBusinessPartnerClaim("foo");
    final boolean isEqualsTrue = validation.evaluate(Operator.EQ, "foo", policyContext);

    // prepare contains
    prepareBusinessPartnerClaim("foobar");
    final boolean isContainedTrue = validation.evaluate(Operator.EQ, "foo", policyContext);

    // assert
    Assertions.assertTrue(isEqualsTrue);
    Assertions.assertTrue(isContainedTrue);
  }

  @Test
  void testValidationWhenParticipantHasProblems() {

    // prepare
    prepareContextProblems(Collections.singletonList("big problem"));
    prepareBusinessPartnerClaim("foo");

    // invoke
    final boolean isValid = validation.evaluate(Operator.EQ, "foo", policyContext);

    // Mockito.verify(monitor.debug(Mockito.anyString());
    Assertions.assertFalse(isValid);
  }

  @Test
  void testValidationWhenSingleParticipantIsValid() {

    // prepare
    prepareContextProblems(null);
    prepareBusinessPartnerClaim("foo");

    // invoke
    final boolean isContainedTrue = validation.evaluate(Operator.EQ, "foo", policyContext);

    // Mockito.verify(monitor.debug(Mockito.anyString());
    Assertions.assertTrue(isContainedTrue);
  }

  @Test
  void testValidationForMultipleParticipants() {

    // prepare
    prepareContextProblems(null);
    prepareBusinessPartnerClaim("foo");

    // invoke & verify
    Assertions.assertTrue(validation.evaluate(Operator.IN, List.of("foo", "bar"), policyContext));
    Assertions.assertTrue(validation.evaluate(Operator.IN, List.of(1, "foo"), policyContext));
    Assertions.assertFalse(validation.evaluate(Operator.IN, List.of("bar", "bar"), policyContext));
  }

  private void prepareContextProblems(List<String> problems) {
    Mockito.when(policyContext.getProblems()).thenReturn(problems);

    if (problems == null || problems.isEmpty()) {
      Mockito.when(policyContext.hasProblems()).thenReturn(false);
    } else {
      Mockito.when(policyContext.hasProblems()).thenReturn(true);
    }
  }

  private void prepareBusinessPartnerClaim(String businessPartnerNumber) {
    Mockito.when(participantAgent.getClaims())
        .thenReturn(Collections.singletonMap("referringConnector", businessPartnerNumber));
  }
}
