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
package org.eclipse.tractusx.edc.oauth2.jwt.decorator;

import java.util.Map;
import java.util.UUID;
import org.eclipse.dataspaceconnector.spi.jwt.JwtDecorator;

public class JtiJwtDecorator implements JwtDecorator {

  @Override
  public Map<String, Object> claims() {
    return Map.of(JWTClaimNames.JWT_ID, UUID.randomUUID().toString());
  }

  @Override
  public Map<String, Object> headers() {
    return Map.of();
  }
}
