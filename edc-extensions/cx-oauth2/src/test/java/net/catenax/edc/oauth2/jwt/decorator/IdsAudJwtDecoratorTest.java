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
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class IdsAudJwtDecoratorTest {

  @Test
  void decorate() {
    final String expectedAudience = "idsc:IDS_CONNECTORS_ALL";
    final IdsAudJwtDecorator decorator = new IdsAudJwtDecorator();

    Assertions.assertTrue(decorator.claims().containsKey(JWTClaimNames.AUDIENCE));
    Assertions.assertEquals(
        List.of(expectedAudience), decorator.claims().get(JWTClaimNames.AUDIENCE));
  }
}
