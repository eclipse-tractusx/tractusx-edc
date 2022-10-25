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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DapsJwtDecoratorTest {

  @Test
  void decorate() {
    final DapsJwtDecorator decorator = new DapsJwtDecorator();

    Assertions.assertTrue(decorator.claims().containsKey("@context"));
    Assertions.assertEquals(
        "https://w3id.org/idsa/contexts/context.jsonld", decorator.claims().get("@context"));

    Assertions.assertTrue(decorator.claims().containsKey("@type"));
    Assertions.assertEquals("ids:DatRequestToken", decorator.claims().get("@type"));
  }
}
