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
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DapsJwtDecoratorTest {

  @Test
  void decorate() {
    final DapsJwtDecorator decorator = new DapsJwtDecorator();

    final JWSHeader.Builder jwsHeaderBuilder = Mockito.mock(JWSHeader.Builder.class);
    final JWTClaimsSet.Builder claimsSetBuilder = Mockito.mock(JWTClaimsSet.Builder.class);

    Mockito.when(claimsSetBuilder.claim(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(claimsSetBuilder);

    decorator.decorate(jwsHeaderBuilder, claimsSetBuilder);

    Mockito.verify(claimsSetBuilder, Mockito.times(1))
        .claim("@context", "https://w3id.org/idsa/contexts/context.jsonld");
    Mockito.verify(claimsSetBuilder, Mockito.times(1)).claim("@type", "ids:DatRequestToken");
    Mockito.verifyNoMoreInteractions(jwsHeaderBuilder, claimsSetBuilder);
  }
}
