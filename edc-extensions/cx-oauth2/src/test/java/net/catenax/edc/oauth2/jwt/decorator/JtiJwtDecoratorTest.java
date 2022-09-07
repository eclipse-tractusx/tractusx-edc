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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class JtiJwtDecoratorTest {

  @Test
  void decorate() {
    final JtiJwtDecorator decorator = new JtiJwtDecorator();

    final JWSHeader.Builder jwsHeaderBuilder = Mockito.mock(JWSHeader.Builder.class);
    final JWTClaimsSet.Builder claimsSetBuilder = new JWTClaimsSet.Builder();

    decorator.decorate(jwsHeaderBuilder, claimsSetBuilder);

    JWTClaimsSet jwtClaimsSet = claimsSetBuilder.build();
    Assertions.assertNotNull(jwtClaimsSet.getJWTID());
  }
}
