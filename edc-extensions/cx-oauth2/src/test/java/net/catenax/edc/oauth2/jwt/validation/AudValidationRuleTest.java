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
package net.catenax.edc.oauth2.jwt.validation;

import java.util.List;
import java.util.Map;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class AudValidationRuleTest {

  private static final String AUDIENCE = "audience";

  private AudValidationRule rule;

  @BeforeEach
  public void setup() {
    final Monitor monitor = Mockito.mock(Monitor.class);
    rule = new AudValidationRule(AUDIENCE, monitor);
  }

  @Test
  void checkRuleSuccess() {
    final Map<String, Object> claims = Map.of("aud", List.of(AUDIENCE));
    final ClaimToken token = ClaimToken.Builder.newInstance().claims(claims).build();
    Result<Void> result = rule.checkRule(token, null);

    Assertions.assertTrue(result.succeeded());
  }

  @Test
  void checkRuleNoClaims() {
    final Map<String, Object> claims = Map.of();
    final ClaimToken token = ClaimToken.Builder.newInstance().claims(claims).build();
    Result<Void> result = rule.checkRule(token, null);

    Assertions.assertFalse(result.succeeded());
  }

  @Test
  void checkRuleClaimMissing() {
    final Map<String, Object> claims = Map.of("foo", List.of(AUDIENCE));
    final ClaimToken token = ClaimToken.Builder.newInstance().claims(claims).build();
    Result<Void> result = rule.checkRule(token, null);

    Assertions.assertFalse(result.succeeded());
  }

  @Test
  void checkRuleAudNotList() {
    final Map<String, Object> claims = Map.of("aud", AUDIENCE);
    final ClaimToken token = ClaimToken.Builder.newInstance().claims(claims).build();
    Result<Void> result = rule.checkRule(token, null);

    Assertions.assertFalse(result.succeeded());
  }
}
