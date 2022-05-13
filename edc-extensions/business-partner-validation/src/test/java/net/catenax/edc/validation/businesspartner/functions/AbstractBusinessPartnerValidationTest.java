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
import org.eclipse.dataspaceconnector.spi.policy.PolicyContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;

public class AbstractBusinessPartnerValidationTest {

  private AbstractBusinessPartnerValidation validation;

  // mocks
  private Monitor monitor;
  private PolicyContext policyContext;
  private ParticipantAgent participantAgent;

  @BeforeEach
  public void BeforeEach() {
    this.monitor = Mockito.mock(Monitor.class);
    this.policyContext = Mockito.mock(PolicyContext.class);
    this.participantAgent = Mockito.mock(ParticipantAgent.class);

    Mockito.when(policyContext.getParticipantAgent()).thenReturn(participantAgent);

    validation = new AbstractBusinessPartnerValidation(monitor) {};
  }

  @ParameterizedTest
  @EnumSource(Operator.class)
  public void testThrowsOnUnsupportedOperations(Operator operator) {

    if (operator == Operator.EQ) { // only allowed operator
      return;
    }

    // prepare
    prepareContextProblems(null);
    prepareBusinessPartnerClaim("yes");

    // invoke & assert
    Assertions.assertThrows(
        UnsupportedOperationException.class,
        () -> validation.evaluate(operator, "null", policyContext));
  }

  @Test
  public void testThrowsOnUnsupportedRightValue() {

    // prepare
    prepareContextProblems(null);
    prepareBusinessPartnerClaim("yes");

    // invoke & assert
    Assertions.assertThrows(
        UnsupportedOperationException.class,
        () -> validation.evaluate(Operator.EQ, 1, policyContext));
    Assertions.assertThrows(
        UnsupportedOperationException.class,
        () -> validation.evaluate(Operator.EQ, new Object(), policyContext));
  }

  @Test
  public void testValidationFailsWhenClaimMissing() {

    // prepare
    prepareContextProblems(null);

    // invoke
    final boolean isValid = validation.evaluate(Operator.EQ, "foo", policyContext);

    // assert
    Assertions.assertFalse(isValid);
  }

  @Test
  public void testValidationSuccedesWhenClaimContainsNumber() {

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
  public void testValidationWhenParticipantHasProblems() {

    // prepare
    prepareContextProblems(Collections.singletonList("big problem"));
    prepareBusinessPartnerClaim("foo");

    // invoke
    final boolean isValid = validation.evaluate(Operator.EQ, "foo", policyContext);

    // Mockito.verify(monitor.debug(Mockito.anyString());
    Assertions.assertFalse(isValid);
  }

  @Test
  public void testValidationWhenParticipantIsValid() {

    // prepare
    prepareContextProblems(null);
    prepareBusinessPartnerClaim("foo");

    // invoke
    final boolean isContainedTrue = validation.evaluate(Operator.EQ, "foo", policyContext);

    // Mockito.verify(monitor.debug(Mockito.anyString());
    Assertions.assertTrue(isContainedTrue);
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
