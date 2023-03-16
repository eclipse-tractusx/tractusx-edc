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

import java.util.Collections;
import java.util.List;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.spi.agent.ParticipantAgent;
import org.eclipse.edc.spi.monitor.Monitor;
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

    if (operator == Operator.EQ) { // only allowed operator
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

  // In the past it was possible to use the 'IN' constraint with multiple BPNs as
  // a list. This is no longer supported.
  // The EDC must now always decline this kind of BPN format.
  @Test
  void testValidationForMultipleParticipants() {

    // prepare
    prepareContextProblems(null);
    prepareBusinessPartnerClaim("foo");

    // invoke & verify
    Assertions.assertFalse(validation.evaluate(Operator.IN, List.of("foo", "bar"), policyContext));
    Assertions.assertFalse(validation.evaluate(Operator.IN, List.of(1, "foo"), policyContext));
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
