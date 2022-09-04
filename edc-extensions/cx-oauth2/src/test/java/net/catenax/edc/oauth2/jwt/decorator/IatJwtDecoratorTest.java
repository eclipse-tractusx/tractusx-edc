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

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
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

    final JWSHeader.Builder jwsHeaderBuilder = Mockito.mock(JWSHeader.Builder.class);
    final JWTClaimsSet.Builder claimsSetBuilder = new JWTClaimsSet.Builder();

    Mockito.when(clock.instant()).thenReturn(Instant.ofEpochSecond(0));
    decorator.decorate(jwsHeaderBuilder, claimsSetBuilder);

    JWTClaimsSet jwtClaimsSet = claimsSetBuilder.build();
    Assertions.assertNotNull(jwtClaimsSet.getIssueTime());
    Assertions.assertEquals(new Date(0), jwtClaimsSet.getIssueTime());
  }

  @Test
  void constructorNull() {
    Assertions.assertThrows(NullPointerException.class, () -> new IatJwtDecorator(null));
  }
}
