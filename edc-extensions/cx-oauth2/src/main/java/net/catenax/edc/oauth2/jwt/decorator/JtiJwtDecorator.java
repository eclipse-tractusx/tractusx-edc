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

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import java.util.UUID;
import lombok.NonNull;
import org.eclipse.dataspaceconnector.spi.jwt.JwtDecorator;

public class JtiJwtDecorator implements JwtDecorator {

  @Override
  public void decorate(
      @NonNull final JWSHeader.Builder header, @NonNull final JWTClaimsSet.Builder claimsSet) {
    claimsSet.jwtID(UUID.randomUUID().toString());
  }
}
