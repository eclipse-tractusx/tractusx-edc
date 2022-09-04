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

package net.catenax.edc.oauth2.jwt.validation;

import java.util.ArrayList;
import java.util.List;
import lombok.NoArgsConstructor;
import org.eclipse.dataspaceconnector.iam.oauth2.spi.Oauth2ValidationRulesRegistry;
import org.eclipse.dataspaceconnector.spi.jwt.TokenValidationRule;
import org.eclipse.dataspaceconnector.spi.jwt.TokenValidationRulesRegistry;

/** Registry for Oauth2 validation rules. */
@NoArgsConstructor
public class Oauth2ValidationRulesRegistryImpl
    implements Oauth2ValidationRulesRegistry, TokenValidationRulesRegistry {

  private final List<TokenValidationRule> rules = new ArrayList<>();

  @Override
  public void addRule(TokenValidationRule rule) {
    rules.add(rule);
  }

  @Override
  public List<TokenValidationRule> getRules() {
    return new ArrayList<>(rules);
  }
}
