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
package org.eclipse.tractusx.edc.oauth2.jwt.decorator;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class IatJwtDecoratorTest {

  @Test
  void decorate() {
    final Clock clock = Mockito.mock(Clock.class);

    final IatJwtDecorator decorator = new IatJwtDecorator(clock);

    Mockito.when(clock.instant()).thenReturn(Instant.ofEpochSecond(0));

    Assertions.assertTrue(decorator.claims().containsKey(JWTClaimNames.ISSUED_AT));
    Assertions.assertEquals(new Date(0), decorator.claims().get(JWTClaimNames.ISSUED_AT));
  }

  @Test
  void constructorNull() {
    Assertions.assertThrows(NullPointerException.class, () -> new IatJwtDecorator(null));
  }
}
