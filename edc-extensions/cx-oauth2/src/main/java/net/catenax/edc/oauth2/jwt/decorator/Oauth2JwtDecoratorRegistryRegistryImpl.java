/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - Initial implementation
 *
 */

package net.catenax.edc.oauth2.jwt.decorator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.eclipse.dataspaceconnector.iam.oauth2.spi.Oauth2JwtDecoratorRegistry;
import org.eclipse.dataspaceconnector.spi.jwt.JwtDecorator;

/** Registry for Oauth2 JWT decorators. */
public class Oauth2JwtDecoratorRegistryRegistryImpl implements Oauth2JwtDecoratorRegistry {
  private final List<JwtDecorator> list = new CopyOnWriteArrayList<>();

  @Override
  public void register(final JwtDecorator decorator) {
    list.add(decorator);
  }

  @Override
  public void unregister(final JwtDecorator decorator) {
    list.remove(decorator);
  }

  @Override
  public Collection<JwtDecorator> getAll() {
    return new ArrayList<>(list);
  }
}
