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

import java.util.Arrays;
import java.util.Map;
import org.eclipse.dataspaceconnector.spi.jwt.JwtDecorator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class Oauth2JwtDecoratorRegistryRegistryImplTest {

  private final Oauth2JwtDecoratorRegistryRegistryImpl oauth2JwtDecoratorRegistryRegistry =
      new Oauth2JwtDecoratorRegistryRegistryImpl();

  @Test
  void test() {
    final A_JwtDecorator a = new A_JwtDecorator();
    final B_JwtDecorator b = new B_JwtDecorator();
    final C_JwtDecorator c = new C_JwtDecorator();

    oauth2JwtDecoratorRegistryRegistry.register(a);

    Assertions.assertNotNull(oauth2JwtDecoratorRegistryRegistry.getAll());
    Assertions.assertEquals(1, oauth2JwtDecoratorRegistryRegistry.getAll().size());

    oauth2JwtDecoratorRegistryRegistry.register(b);

    Assertions.assertNotNull(oauth2JwtDecoratorRegistryRegistry.getAll());
    Assertions.assertEquals(2, oauth2JwtDecoratorRegistryRegistry.getAll().size());

    oauth2JwtDecoratorRegistryRegistry.register(c);

    Assertions.assertNotNull(oauth2JwtDecoratorRegistryRegistry.getAll());
    Assertions.assertEquals(3, oauth2JwtDecoratorRegistryRegistry.getAll().size());

    Assertions.assertTrue(
        oauth2JwtDecoratorRegistryRegistry.getAll().containsAll(Arrays.asList(a, b, c)));

    oauth2JwtDecoratorRegistryRegistry.unregister(c);

    Assertions.assertNotNull(oauth2JwtDecoratorRegistryRegistry.getAll());
    Assertions.assertEquals(2, oauth2JwtDecoratorRegistryRegistry.getAll().size());

    Assertions.assertTrue(
        oauth2JwtDecoratorRegistryRegistry.getAll().containsAll(Arrays.asList(a, b)));

    oauth2JwtDecoratorRegistryRegistry.unregister(b);

    Assertions.assertNotNull(oauth2JwtDecoratorRegistryRegistry.getAll());
    Assertions.assertEquals(1, oauth2JwtDecoratorRegistryRegistry.getAll().size());

    Assertions.assertTrue(oauth2JwtDecoratorRegistryRegistry.getAll().contains(a));

    oauth2JwtDecoratorRegistryRegistry.unregister(a);

    Assertions.assertNotNull(oauth2JwtDecoratorRegistryRegistry.getAll());
    Assertions.assertTrue(oauth2JwtDecoratorRegistryRegistry.getAll().isEmpty());
  }

  private static class A_JwtDecorator implements JwtDecorator {
    @Override
    public Map<String, Object> claims() {
      return null;
    }

    @Override
    public Map<String, Object> headers() {
      return null;
    }
  }

  private static class B_JwtDecorator implements JwtDecorator {
    @Override
    public Map<String, Object> claims() {
      return null;
    }

    @Override
    public Map<String, Object> headers() {
      return null;
    }
  }

  private static class C_JwtDecorator implements JwtDecorator {
    @Override
    public Map<String, Object> claims() {
      return null;
    }

    @Override
    public Map<String, Object> headers() {
      return null;
    }
  }
}
