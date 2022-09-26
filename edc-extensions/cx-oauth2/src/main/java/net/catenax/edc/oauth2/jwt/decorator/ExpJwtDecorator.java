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

import com.nimbusds.jwt.JWTClaimNames;
import java.time.Clock;
import java.time.Duration;
import java.util.Date;
import java.util.Map;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.eclipse.dataspaceconnector.spi.jwt.JwtDecorator;

@RequiredArgsConstructor
public class ExpJwtDecorator implements JwtDecorator {
  @NonNull private final Clock clock;

  @NonNull private final Duration expiration;

  @Override
  public Map<String, Object> claims() {
    return Map.of(
        JWTClaimNames.EXPIRATION_TIME,
        Date.from(clock.instant().plusSeconds(expiration.toSeconds())));
  }

  @Override
  public Map<String, Object> headers() {
    return Map.of();
  }
}
