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
 *       Mercedes-Benz Tech Innovation GmbH - Initial API and Implementation
 *
 */
package net.catenax.edc.oauth2.jwt.decorator;

import java.util.Collections;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.eclipse.dataspaceconnector.spi.jwt.JwtDecorator;

@RequiredArgsConstructor
public class IdsAudJwtDecorator implements JwtDecorator {

  @Override
  public Map<String, Object> claims() {
    return Map.of(JWTClaimNames.AUDIENCE, Collections.singletonList("idsc:IDS_CONNECTORS_ALL"));
  }

  @Override
  public Map<String, Object> headers() {
    return Map.of();
  }
}
