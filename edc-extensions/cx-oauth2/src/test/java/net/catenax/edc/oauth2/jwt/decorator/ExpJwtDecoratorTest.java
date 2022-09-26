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
 *       Mercedes-Benz Tech Innovation GmbH - Initial Test
 *
 */
package net.catenax.edc.oauth2.jwt.decorator;

import com.nimbusds.jwt.JWTClaimNames;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ExpJwtDecoratorTest {

  @Test
  void decorate() {
    final Clock clock = Mockito.mock(Clock.class);
    final Duration expiration = Duration.ofSeconds(100);

    final ExpJwtDecorator decorator = new ExpJwtDecorator(clock, expiration);

    Mockito.when(clock.instant()).thenReturn(Instant.ofEpochSecond(0));

    Assertions.assertTrue(decorator.claims().containsKey(JWTClaimNames.EXPIRATION_TIME));
    Assertions.assertEquals(
        new Date(100000), decorator.claims().get(JWTClaimNames.EXPIRATION_TIME));
  }

  @Test
  void constructorNull() {
    Assertions.assertThrows(NullPointerException.class, () -> new ExpJwtDecorator(null, null));
  }
}
