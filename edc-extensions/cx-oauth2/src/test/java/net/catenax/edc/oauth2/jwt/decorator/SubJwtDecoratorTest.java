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
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SubJwtDecoratorTest {

  @Test
  void decorate() {
    final String expectedSubject = UUID.randomUUID().toString();
    final SubJwtDecorator decorator = new SubJwtDecorator(expectedSubject);

    final JWSHeader.Builder jwsHeaderBuilder = Mockito.mock(JWSHeader.Builder.class);
    final JWTClaimsSet.Builder claimsSetBuilder = Mockito.mock(JWTClaimsSet.Builder.class);

    decorator.decorate(jwsHeaderBuilder, claimsSetBuilder);

    Mockito.verify(claimsSetBuilder, Mockito.times(1)).subject(expectedSubject);
    Mockito.verifyNoMoreInteractions(jwsHeaderBuilder, claimsSetBuilder);
  }

  @Test
  void constructorNull() {
    Assertions.assertThrows(NullPointerException.class, () -> new SubJwtDecorator(null));
  }
}
