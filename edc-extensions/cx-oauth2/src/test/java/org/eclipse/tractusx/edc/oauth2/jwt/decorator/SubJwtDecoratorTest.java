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

import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SubJwtDecoratorTest {

  @Test
  void decorate() {
    final String expectedSubject = UUID.randomUUID().toString();
    final SubJwtDecorator decorator = new SubJwtDecorator(expectedSubject);

    Assertions.assertTrue(decorator.claims().containsKey(JWTClaimNames.SUBJECT));
    Assertions.assertEquals(expectedSubject, decorator.claims().get(JWTClaimNames.SUBJECT));
  }

  @Test
  void constructorNull() {
    Assertions.assertThrows(NullPointerException.class, () -> new SubJwtDecorator(null));
  }
}
